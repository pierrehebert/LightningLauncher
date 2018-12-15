package org.mozilla.javascript.regexp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

public class JreNativeRegExp extends IdScriptableObject implements Function {
    static final long serialVersionUID = 4965263491464903264L;
    private static final Object REGEXP_TAG = new Object();

    RE re = new RE();
    double lastIndex;          /* index after last match, for //g iterator */

    public static void init(Context cx, Scriptable scope, boolean sealed) {
        JreNativeRegExp obj = new JreNativeRegExp();
        obj.re.pattern = Pattern.compile("");
        obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
    }

    JreNativeRegExp(Scriptable scope, Object compiled) {
        this.re = (RE) compiled;
        this.lastIndex = 0;
        ScriptRuntime.setObjectProtoAndParent(this, scope);
    }

    @Override
    public String getClassName() {
        return "RegExp";
    }

    /**
     * Gets the value to be returned by the typeof operator called on this object.
     * @see org.mozilla.javascript.ScriptableObject#getTypeOf()
     * @return "object"
     */
    @Override
    public String getTypeOf() {
    	return "object";
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj,
                       Object[] args) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        throw new UnsupportedOperationException();
    }

    Scriptable compile(Context cx, Scriptable scope, Object[] args) {
        if (args.length > 0 && args[0] instanceof JreNativeRegExp) {
            if (args.length > 1 && args[1] != Undefined.instance) {
                // report error
                throw ScriptRuntime.typeError0("msg.bad.regexp.compile");
            }
            JreNativeRegExp thatObj = (JreNativeRegExp) args[0];
            this.re = thatObj.re;
            this.lastIndex = thatObj.lastIndex;
            return this;
        }
        String regexp = args.length == 0 ? "" : ScriptRuntime.toString(args[0]);
        String flags = args.length > 1 && args[1] != Undefined.instance
            ? ScriptRuntime.toString(args[1])
            : null;
        this.re = compileRE(cx, regexp, flags);
        this.lastIndex = 0;
        return this;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append('/');
        if (re.source.length() > 0) {
            buf.append(re.source);
        } else {
            // See bugzilla 226045
            buf.append("(?:)");
        }
        buf.append('/').append(re.flags);
        return buf.toString();
    }

    JreNativeRegExp() {  }

    static RE compileRE(Context cx, String regexp, String flags) {
        if (flags == null) {
            flags = "";
        }
        RE re = new RE();
        re.source = regexp;
        re.flags = flags;
        int iflags = 0;
        for (int i = 0; i < flags.length(); i++) {
            char c = flags.charAt(i);
            if (c == 'g') {
                re.globalFlag = true;
            } else if (c == 'i') {
                iflags |= Pattern.CASE_INSENSITIVE;
            } else if (c == 'm') {
                iflags |= Pattern.MULTILINE;
            } else {
                reportError("msg.invalid.re.flag", String.valueOf(c));
            }
        }

        // Translate regexp from js to java.
        // Some symbols have different translations depending on whether
        // they are used as regular characters (c), or inside a character
        // class in either positive (cc+) or negated (cc-) form.
        // 1.*     '\0'  => '\x00'
        // 2.*     '\v'  => '\x0b'
        // 3.*     '[^]' => '[\s\S]'
        // 4.      '\b':
        //   c)          => '\b'
        //   cc+-)       => '\x08'
        // 5.      '\s':
        //   c)          => '[\s\ufeff]'
        //   cc+-)       => '\s\ufeff'
        // 6.      '\S':
        //   c)          => '[\S&&[^\ufeff]]'
        //   cc+)        => '\S&&[^\ufeff]'
        //   cc-)        => '\S[\ufeff]'
        // 7.      '[':
        //   cc+-)       => '\['

        StringBuilder jrepattern = new StringBuilder();
        boolean inCC = false; // inside character class
        boolean negCC = false; // negated character class
        for (int i = 0; i < regexp.length(); i++) {
            char c = regexp.charAt(i);
            switch (c) {
            case '\\': // escape-sequence
                if (regexp.length() <= i) { // dangling slash at end of regexp
                    jrepattern.append('\\');
                    break;
                }
                c = regexp.charAt(++i);
                switch (c) {
                case '0': // 1.* '\0' => '\x00'
                    jrepattern.append("\\x00");
                    break;
                case 'v': // 2.* '\v' => '\x0b'
                    jrepattern.append("\\x0b");
                    break;
                case 'b':
                    if (inCC) { // 4.cc+- '\b' => '\x08'
                        jrepattern.append("\\x08");
                    } else {    // 4.c    '\b' => '\b'
                        jrepattern.append("\\b");
                    }
                    break;
                case 's':
                    if (inCC) { // 5.cc+- '\s' => '\s\ufeff'
                        jrepattern.append("\\s\\ufeff");
                    } else {    // 5.c    '\s' => '[\s\ufeff]'
                        jrepattern.append("[\\s\\ufeff]");
                    }
                    break;
                case 'S':
                    if (inCC) {
                        if (negCC) { // 6.cc- '\S' => '\S[\ufeff]'
                            jrepattern.append("\\S[\\ufeff]");
                        } else {     // 6.cc+ '\S' => '\S&&[^\ufeff]'
                            jrepattern.append("\\S&&[^\\ufeff]");
                        }
                    } else {         // 6.c   '\S' => '[\S&&[^\ufeff]]'
                        jrepattern.append("[\\S&&[^\\ufeff]]");
                    }
                    break;
                default: // leave the escape 'as-is'
                    jrepattern.append('\\').append(c);
                }
                break;
            case '[':
                if (inCC) { // 7.cc+- '[' => '\['
                    jrepattern.append("\\[");
                    break;
                }

                // start of character class, check if negated
                if (regexp.length() > i+1 && regexp.charAt(i+1) == '^') {
                    // check for special match of '[^]'
                    if (regexp.length() > i+2 && regexp.charAt(i+2) == ']') {
                        // 3.* '[^]' => '[\s\S]'
                        jrepattern.append("[\\s\\S]");
                        i += 2;
                        break;
                    }
                    negCC = true;
                }
                inCC = true;
                jrepattern.append('[');
                break;
            case ']':
                // end of character class
                inCC = false;
                negCC = false;
                jrepattern.append(']');
                break;
            default:
                jrepattern.append(c);
            }
        }

        re.pattern = Pattern.compile(jrepattern.toString(), iflags);
        return re;
    }

    private static void reportWarning(Context cx, String messageId, String arg) {
        if (cx.hasFeature(Context.FEATURE_STRICT_MODE)) {
            String msg = ScriptRuntime.getMessage1(messageId, arg);
            Context.reportWarning(msg);
        }
    }

    private static void reportError(String messageId, String arg) {
        String msg = ScriptRuntime.getMessage1(messageId, arg);
        throw ScriptRuntime.constructError("SyntaxError", msg);
    }

// #string_id_map#

    private static final int
        Id_lastIndex    = 1,
        Id_source       = 2,
        Id_global       = 3,
        Id_ignoreCase   = 4,
        Id_multiline    = 5,

        MAX_INSTANCE_ID = 5;

    @Override
    protected int getMaxInstanceId() {
        return MAX_INSTANCE_ID;
    }

    @Override
    protected int findInstanceIdInfo(String s) {
        int id;
// #generated# Last update: 2007-05-09 08:16:24 EDT
        L0: { id = 0; String X = null; int c;
            int s_length = s.length();
            if (s_length==6) {
                c=s.charAt(0);
                if (c=='g') { X="global";id=Id_global; }
                else if (c=='s') { X="source";id=Id_source; }
            }
            else if (s_length==9) {
                c=s.charAt(0);
                if (c=='l') { X="lastIndex";id=Id_lastIndex; }
                else if (c=='m') { X="multiline";id=Id_multiline; }
            }
            else if (s_length==10) { X="ignoreCase";id=Id_ignoreCase; }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
// #/string_id_map#

        if (id == 0) return super.findInstanceIdInfo(s);

        int attr;
        switch (id) {
          case Id_lastIndex:
            attr = PERMANENT | DONTENUM;
            break;
          case Id_source:
          case Id_global:
          case Id_ignoreCase:
          case Id_multiline:
            attr = PERMANENT | READONLY | DONTENUM;
            break;
          default:
            throw new IllegalStateException();
        }
        return instanceIdInfo(attr, id);
    }

    @Override
    protected String getInstanceIdName(int id) {
        switch (id) {
            case Id_lastIndex:  return "lastIndex";
            case Id_source:     return "source";
            case Id_global:     return "global";
            case Id_ignoreCase: return "ignoreCase";
            case Id_multiline:  return "multiline";
        }
        return super.getInstanceIdName(id);
    }

    @Override
    protected Object getInstanceIdValue(int id) {
        switch (id) {
          case Id_lastIndex:
            return ScriptRuntime.wrapNumber(lastIndex);
          case Id_source:
            return re.source;
          case Id_global:
            return ScriptRuntime.wrapBoolean(re.globalFlag);
          case Id_ignoreCase:
            return ScriptRuntime.wrapBoolean((re.pattern.flags() & Pattern.CASE_INSENSITIVE) != 0);
          case Id_multiline:
            return ScriptRuntime.wrapBoolean((re.pattern.flags() & Pattern.MULTILINE) != 0);
        }
        return super.getInstanceIdValue(id);
    }

    @Override
    protected void setInstanceIdValue(int id, Object value) {
        switch (id) {
          case Id_lastIndex:
            lastIndex = ScriptRuntime.toNumber(value);
            return;
          case Id_source:
          case Id_global:
          case Id_ignoreCase:
          case Id_multiline:
            return;
        }
        super.setInstanceIdValue(id, value);
    }

    @Override
    protected void initPrototypeId(int id) {
        String s;
        int arity;
        switch (id) {
          case Id_constructor: arity=1; s="constructor"; break;
          case Id_compile:     arity=1; s="compile";     break;
          case Id_toString:    arity=0; s="toString";    break;
          case Id_toSource:    arity=0; s="toSource";    break;
          case Id_exec:        arity=1; s="exec";        break;
          case Id_test:        arity=1; s="test";        break;
          default: throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(REGEXP_TAG, id, s, arity);
    }

    @Override
    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
                             Scriptable thisObj, Object[] args) {
        if (!f.hasTag(REGEXP_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
          case Id_constructor:
            JreNativeRegExp newObj = new JreNativeRegExp();
            ScriptRuntime.setObjectProtoAndParent(newObj, scope);
            thisObj = newObj; // fall through to compile
          case Id_compile:
            return realThis(thisObj, f).compile(cx, scope, args);

          case Id_toString:
          case Id_toSource:
            return realThis(thisObj, f).toString();

          case Id_exec:
              if (args.length == 0) {
                  reportError("msg.no.re.input.for", toString());
              }
              return js_exec(cx, scope, realThis(thisObj, f),
                      ScriptRuntime.toString(args[0]));

          case Id_test: {
              if (args.length == 0) {
                  reportError("msg.no.re.input.for", toString());
              }
              return ScriptRuntime.wrapBoolean(
                      realThis(thisObj, f).re.pattern.matcher(
                              ScriptRuntime.toString(args[0])).find());
          }

        }
        throw new IllegalArgumentException(String.valueOf(id));
    }

    private static JreNativeRegExp realThis(Scriptable thisObj, IdFunctionObject f) {
        if (!(thisObj instanceof JreNativeRegExp))
            throw incompatibleCallError(f);
        return (JreNativeRegExp)thisObj;
    }

// #string_id_map#
    @Override
    protected int findPrototypeId(String s) {
        int id;
// #generated# Last update: 2007-05-09 08:16:24 EDT
        L0: { id = 0; String X = null; int c;
            L: switch (s.length()) {
            case 4: c=s.charAt(0);
                if (c=='e') { X="exec";id=Id_exec; }
                else if (c=='t') { X="test";id=Id_test; }
                break L;
            case 7: X="compile";id=Id_compile; break L;
            case 8: c=s.charAt(3);
                if (c=='o') { X="toSource";id=Id_toSource; }
                else if (c=='t') { X="toString";id=Id_toString; }
                break L;
            case 11: X="constructor";id=Id_constructor; break L;
            }
            if (X!=null && X!=s && !X.equals(s)) id = 0;
            break L0;
        }
// #/generated#
        return id;
    }

    private static final int
        Id_constructor   = 1,
        Id_compile       = 2,
        Id_toString      = 3,
        Id_toSource      = 4,
        Id_exec          = 5,
        Id_test          = 6,

        MAX_PROTOTYPE_ID = 6;

// #/string_id_map#


    static Scriptable js_exec(Context cx, Scriptable scope,
            JreNativeRegExp re, String str) {

        // if global flag, exec starts from 'lastIndex'
        // check str is long enough if we are going from 'lastIndex'
        if (re.re.globalFlag && re.lastIndex >= str.length()) {
            re.lastIndex = 0;
            return null;
        }
        Matcher m = re.re.pattern.matcher(str);
        if (!m.find((int) re.lastIndex)) {
            re.lastIndex = 0;
            return null;
        }

        // update 'lastIndex' if global
        if (re.re.globalFlag) {
            re.lastIndex = m.end();
        }

        // put results into array
        Scriptable result = cx.newObject(scope, "Array");
        for (int i = 0; i <= m.groupCount(); i++) {
            result.put(i, result, m.group(i));
        }

        // put 'index' and 'input'
        result.put("index", result, m.start());
        result.put("input", result, str);
        return result;
    }
}       // class JdkNativeRegExp

class RE {
    String source;
    String flags;
    Pattern pattern;
    boolean globalFlag = false;
}
