package org.mozilla.javascript.regexp;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.RegExpProxy;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class JreRegExpImpl implements RegExpProxy {

    @Override
    public boolean isRegExp(Scriptable obj) {
        return obj instanceof JreNativeRegExp;
    }

    @Override
    public Object compileRegExp(Context cx, String source, String flags) {
        return JreNativeRegExp.compileRE(cx, source, flags);
    }

    @Override
    public Scriptable wrapRegExp(Context cx, Scriptable scope, Object regexp) {
        return new JreNativeRegExp(scope, regexp);
    }

    @Override
    public Object action(Context cx, Scriptable scope, Scriptable thisObj,
            Object[] args, int actionType) {

        String thisStr = ScriptRuntime.toString(thisObj);
        JreNativeRegExp re;
        Matcher m;
        switch (actionType) {
        case RA_MATCH:
            // str.match(regexp)
            // if not global flag, then RegExp.exec, else array with all matches
            re = args2regexp(cx, scope, args, 0, -1, false);
            if (!re.re.globalFlag) {
                return JreNativeRegExp.js_exec(cx, scope, re, thisStr);
            }

            // array with all matches
            m = re.re.pattern.matcher(thisStr);
            List<String> matchResult = new ArrayList<String>();
            while (m.find()) {
                matchResult.add(m.group());
            }
            return matchResult.size() > 0
                ? cx.newArray(scope, matchResult.toArray()) : null;

        case RA_SEARCH:
            // pos = search(regexp);
            re = args2regexp(cx, scope, args, 0, -1, false);
            m = re.re.pattern.matcher(thisStr);
            if (m.find()) {
                return m.start();
            } else {
                return -1;
            }
        case RA_REPLACE:
            // str.replace(regexp|substr, newSubStr|function[, Non-standard flags]);
            // treat substr as string, not regexp, optional flags in args[0]
            re = args2regexp(cx, scope, args, 0, 2, true);
            m = re.re.pattern.matcher(thisStr);

            String newSubStr = null;
            Function f = null;
            Scriptable parent = ScriptableObject.getTopLevelScope(scope);
            if (args.length > 1 && args[1] instanceof Function) {
                f = (Function) args[1];
            } else {
                newSubStr = ScriptRuntime.toString(
                        args.length > 1 ? args[1] : Undefined.instance);
            }

            StringBuilder replaceResult = new StringBuilder();
            int searchStart = 0;
            while (m.find()) {
                // copy from searchStart to matchStart
                replaceResult.append(thisStr, searchStart, m.start());
                searchStart = m.end(); // update searchStart

                // if f: call f with str, p1, p2, ..., offset, s
                if (f != null) {
                    Object[] fargs = new Object[3 + m.groupCount()];
                    for (int i = 0; i <= m.groupCount(); i++) {
                        fargs[i] = m.group(i);
                    }
                    fargs[fargs.length -2] = m.start();
                    fargs[fargs.length - 1] = thisStr;
                    Object replacement = f.call(cx, scope, parent, fargs);

                    // copy replacement
                    replaceResult.append(ScriptRuntime.toString(replacement));
                } else {
                    // substitute newSubStr
                    for (int i = 0; i < newSubStr.length();) {
                        char c = newSubStr.charAt(i++);
                        // not a special char
                        if (c != '$' || newSubStr.length() == i) {
                            replaceResult.append(c);
                            continue;
                        }

                        // special handling for: $$, $&, $`, $', $n, $nn
                        char after$ = newSubStr.charAt(i++);
                        switch (after$) {
                        case '$':
                            replaceResult.append('$');
                            break;
                        case '&':
                            replaceResult.append(m.group());
                            break;
                        case '`':
                            replaceResult.append(thisStr, 0, m.start());
                            break;
                        case '\'':
                            replaceResult.append(
                                    thisStr, m.end(), thisStr.length());
                            break;
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            int n = after$ - '0';
                            // check for $nn
                            if (newSubStr.length() > i) {
                                c = newSubStr.charAt(i);
                                int nn = n * 10 + c - '0';
                                if (c >= '0' && c <= '9' && nn > 0
                                        && nn <= m.groupCount()) {
                                    n = nn;
                                    i++;
                                }
                            }
                            // substitute group
                            if (n > 0 && m.groupCount() >= n) {
                                replaceResult.append(m.group(n));
                                break;
                            }
                            // fall through if no group match
                        default:
                            replaceResult.append('$').append(after$);
                        }
                    }
                }

                // break after 1 match if no global
                if (!re.re.globalFlag) {
                    break;
                }
            }
            // copy from searchStart to end of string
            replaceResult.append(thisStr, searchStart, thisStr.length());

            return replaceResult.toString();

        default:
            throw Kit.codeBug();
        }
    }

    // doesn't seem to be used anywhere
    @Override
    public int find_split(Context cx, Scriptable scope, String target,
            String separator, Scriptable re, int[] ip, int[] matchlen,
            boolean[] matched, String[][] parensp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object js_split(Context _cx, Scriptable _scope, String thisString,
            Object[] _args) {

        // ecma 262 15.5.4.14
        // var splits = str.split([separator][, limit]);

        // step 5 get limit, default = 2^32-1,
        long limit = -1;  // will change to default
        if (_args.length > 1) {
            limit = ScriptRuntime.toInt32(_args[1]);
        }
        // default limit is 2^32 - 1
        limit = limit < 0 ? 0xffffffffL : limit;
        if (limit == 0) {
            return _cx.newArray(_scope, 0);
        }

        // step 10: if sep is undefined, return [thisString]
        if (_args.length == 0) {
            return _cx.newArray(_scope, new Object[] {thisString});
        }

        // step 8: if separator is str, then escape before converting to regexp
        JreNativeRegExp re = args2regexp(_cx, _scope, _args, 0, -1, true);
        Matcher m = re.re.pattern.matcher(thisString);
        List<String> result = new ArrayList<String>();

        // step 11: special handling if thisString == ''
        if (thisString.length() == 0) {
            if (!m.find()) {
                return _cx.newArray(_scope, new Object[] {thisString});
            } else {
                return _cx.newArray(_scope, new Object[] {});
            }
        }

        int searchStart = 0;
        while (result.size() < limit && m.regionStart() < thisString.length()
                && m.find()) {

            // step 13.c.ii, pieces of split must be at least 1 char long
            if (m.end() == searchStart) {
                // move region forward by 1
                m.region(searchStart + 1, thisString.length());
                continue;
            }
            // step 13.c.iii.1, add string from searchStart to matchStart
            result.add(thisString.substring(searchStart, m.start()));
            searchStart = m.end();
            // step 13.c.iii.7, add all capture groups
            for (int i = 1; i <= m.groupCount() && result.size() < limit; i++) {
                result.add(m.group(i));
            }
            // need to reset region to stop empty matches from
            // moving the m.nextSearchIndex position forward
            m.region(searchStart, thisString.length());
        }
        // step 14, push last piece if not over limit
        if (result.size() < limit) {
            result.add(thisString.substring(searchStart));
        }

        return _cx.newArray(_scope, result.toArray());
    }

    /**
     * Converts args to regexp
     * @param cx context
     * @param scope scope
     * @param args args to convert
     * @param regexpIndex index of regexp or convert to string
     * @param flagsIndex index of flags (-1 if no flags)
     * @param escapeStr if true, string is escaped before converted to regexp
     * @return regexp
     */
    private JreNativeRegExp args2regexp(Context cx, Scriptable scope,
            Object[] args, int regexpIndex, int flagsIndex, boolean escapeStr) {

        if (args.length > regexpIndex
                && args[regexpIndex] instanceof JreNativeRegExp) {
            return (JreNativeRegExp) args[regexpIndex];
        }
        String src = args.length > regexpIndex
            ? ScriptRuntime.toString(args[regexpIndex]) : "";
        if (escapeStr) {
            src = Pattern.quote(src);
        }
        String flags = flagsIndex >= 0 && flagsIndex < args.length
            ? ScriptRuntime.toString(args[flagsIndex]) : "";

        return new JreNativeRegExp(ScriptableObject.getTopLevelScope(scope),
                JreNativeRegExp.compileRE(cx, src, flags));
    }
}
