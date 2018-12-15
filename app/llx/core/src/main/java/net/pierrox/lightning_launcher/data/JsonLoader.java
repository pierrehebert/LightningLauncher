package net.pierrox.lightning_launcher.data;

import android.util.Log;

import net.pierrox.lightning_launcher.configuration.GlobalConfig;
import net.pierrox.lightning_launcher.engine.variable.Binding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public class JsonLoader {
    /** Specialized for configuration objects, not a multi-purpose tool. */
	public static JSONObject toJSONObject(Object o, Object default_value) {
        JSONObject json_object=new JSONObject();

        toJSONObject(json_object, o, default_value);

        return json_object;
    }

    /** Specialized for configuration objects, not a multi-purpose tool. */
	public static void toJSONObject(JSONObject json_object, Object o, Object default_value) {
		for(Field f : o.getClass().getFields()) {
			if(Modifier.isFinal(f.getModifiers())) continue;

			if(default_value!=null) {
				try {
					Object f_o = f.get(o);
					Object f_do = f.get(default_value);
					if(f_o==null && f_do==null) continue;
					if(f_o!=null && f_o.equals(f_do)) continue;
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}

			String name=f.getName();

			try {
				Class<?> cls=f.getType();
		        if(cls==boolean.class) {
		        	json_object.put(name, f.getBoolean(o));
		        } else if(cls==int.class) {
		        	json_object.put(name, f.getInt(o));
                } else if(cls==long.class) {
                    json_object.put(name, f.getLong(o));
		        } else if(cls==int[].class) {
		        	int[] ai=(int[])f.get(o);
		        	if(ai!=null) {
		        		JSONArray jai=new JSONArray();
		        		for(int i : ai) {
		        			jai.put(i);
		        		}
		        		json_object.put(name, jai);
		        	}
		        } else if(cls==float.class) {
		        	json_object.put(name, (double)f.getFloat(o));
		        } else if(cls==String.class) {
		        	json_object.put(name, (String)f.get(o));
                } else if(cls==String[].class) {
                    String[] as=(String[])f.get(o);
                    if(as!=null) {
                        JSONArray jas=new JSONArray();
                        for(String s : as) {
                            jas.put(s);
                        }
                        json_object.put(name, jas);
                    }
                } else if(cls==EventAction.class) {
                    EventAction ea = (EventAction) f.get(o);
                    if (ea != null && ea.action != GlobalConfig.UNSET) {
                        JSONObject j_ea = new JSONObject();
                        j_ea.put("a", ea.action);
                        j_ea.put("d", ea.data);
                        if(ea.next != null) {
                            j_ea.put("n", toJSONObject(ea.next, null));
                        }
                        json_object.put(name, j_ea);
                    }
                } else if(cls== Binding[].class) {
                    Binding[] bindings=(Binding[])f.get(o);
                    if(bindings!=null) {
                        JSONArray jbindings=new JSONArray();
                        for(Binding binding : bindings) {
                            JSONObject j = new JSONObject();
                            j.put("t", binding.target);
                            j.put("f", binding.formula);
                            j.put("e", binding.enabled);
                            jbindings.put(j);
                        }
                        json_object.put(name, jbindings);
                    }
                } else if(cls==HashMap.class) {
                    HashMap m = (HashMap) f.get(o);
                    if(m != null) {
                        json_object.put(name, new JSONObject((HashMap)f.get(o)));
                    }
                } else if(cls.isEnum()) {
                    json_object.put(name, f.get(o).toString());
                }
			} catch(IllegalAccessException e) {
				e.printStackTrace();
			} catch (JSONException e) {
                e.printStackTrace();
            }
        }
	}

    public void loadFieldsFromJSONObject(JSONObject json, Object d) {
        loadFieldsFromJSONObject(this, json, d);
    }
    
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void loadFieldsFromJSONObject(Object thiz, JSONObject json, Object d) {
        if(d == null) {
            d = thiz;
        }
		for(Field f : thiz.getClass().getFields()) {
			if(Modifier.isFinal(f.getModifiers())) continue;
			String name=f.getName();
			try {
				
				Class<?> cls=f.getType();
                if(cls==boolean.class) {
                	f.setBoolean(thiz, json.optBoolean(name, f.getBoolean(d)));
                } else if(cls==int.class) {
                	f.setInt(thiz, json.optInt(name, f.getInt(d)));
                } else if(cls==int[].class) {
                	JSONArray a = json.optJSONArray(name);
                	if(a==null) {
                		f.set(thiz, null);
                	} else {
                		int n=a.length();
                		int[] ai=new int[n];
                		for(int i=0; i<n; i++) {
                			ai[i] = a.getInt(i);
                		}
                		f.set(thiz, ai);
                	}
                } else if(cls==float.class) {
                	f.setFloat(thiz, (float)json.optDouble(name, f.getDouble(d)));
                } else if(cls==long.class) {
                	f.setLong(thiz, json.optLong(name, f.getLong(d)));
                } else if(cls==String.class) {
                	f.set(thiz, json.optString(name, (String)f.get(d)));
                } else if(cls==String[].class) {
                    JSONArray a = json.optJSONArray(name);
                    if(a==null) {
                        f.set(thiz, null);
                    } else {
                        int n=a.length();
                        String[] as=new String[n];
                        for(int i=0; i<n; i++) {
                            as[i] = a.getString(i);
                        }
                        f.set(thiz, as);
                    }
                } else if(cls.isEnum()) {
                	String enum_string=json.optString(name, null);
                	if(enum_string!=null) {
                		f.set(thiz, Enum.valueOf((Class<Enum>)cls, enum_string));
                	} else {
                		f.set(thiz, f.get(d));
                	}
                } else if(cls==EventAction.class) {
                    EventAction ea;
                    if (json.has(name)) {
                        try {
                            JSONObject j_ea = json.getJSONObject(name);
                            JSONObject n = j_ea.optJSONObject("n");
                            EventAction next = null;
                            if(n != null) {
                                next = new EventAction();
                                loadFieldsFromJSONObject(next, n, null);
                            }
                            ea = new EventAction(j_ea.getInt("a"), j_ea.optString("d", null), next);
                        } catch (JSONException e) {
                            // compatibility with older version
                            String data = json.optString(name + "Data", null);
                            ea = new EventAction(json.getInt(name), data);
                        }
                    } else {
                        EventAction d_ea = (EventAction) f.get(d);
                        ea = d_ea;
                    }
                    f.set(thiz, ea);
                } else if(cls== Binding[].class) {
                    JSONArray a = json.optJSONArray(name);
                    if(a==null) {
                        f.set(thiz, null);
                    } else {
                        int n=a.length();
                        Binding[] bindings;
                        if(n==0) {
                            bindings = null;
                        } else {
                            bindings = new Binding[n];
                            for (int i = 0; i < n; i++) {
                                JSONObject o = a.getJSONObject(i);
                                Binding b = new Binding(o.getString("t"), o.getString("f"), o.getBoolean("e"));
                                bindings[i] = b;
                            }
                        }
                        f.set(thiz, bindings);
                    }
                } else if(cls==HashMap.class) {
                    JSONObject o = json.optJSONObject(name);
                    if(o != null) {
                        f.set(thiz, Utils.jsonObjectToHashMap(o));
                    } else {
                        f.set(thiz, f.get(d));
                    }
                } else if(cls.getSuperclass()==JsonLoader.class) {
                	JsonLoader j=(JsonLoader)cls.newInstance();
                	JSONObject o2=json.optJSONObject(name);
                	if(o2==null) {
                		o2=new JSONObject();
                	}
                	j.loadFieldsFromJSONObject(o2, f.get(d));
                	f.set(thiz, j);
                }
			} catch(Exception e) {
				Log.i("LL", "bad field "+name);
				// pass
//				e.printStackTrace();
			}
		}
	}

    public void copyFrom(JsonLoader o) {
		try {
			loadFieldsFromJSONObject(toJSONObject(o, null), o);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    public static <T extends JsonLoader> T readObject(Class<T> cls, File from) {
        T instance;
        try {
            instance = cls.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        JSONObject json = FileUtils.readJSONObjectFromFile(from);
        if(json != null) {
            instance.loadFieldsFromJSONObject(json, instance);
        }

        return instance;
    }

    public static boolean saveObjectToFile(Object o, File to) {
        try {
            JSONObject json = JsonLoader.toJSONObject(o, null);
            FileUtils.saveStringToFile(json.toString(), to);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String toString() {
        return JsonLoader.toJSONObject(this, null).toString();
    }
}
