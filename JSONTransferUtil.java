package com.ted.android.utils;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author gaoxuefeng
 * @date 2018/4/10
 */

public class JSONTransferUtil {

    private static boolean DEBUG = false;

    public static <T> T toBean(String jsonStr, Class<T> clazz) {
        try {
            JSONObject job = new JSONObject(jsonStr);
            return parseObject(job, clazz, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> List<T> toBeanList(String jsonStr, Class<T> clazz) {
        if (TextUtils.isEmpty(jsonStr)) {
            return new ArrayList<>();
        }
        List<T> beanList = new ArrayList<>();
        try {
            if (jsonStr.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(jsonStr);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    T t = parseObject(jsonObject, clazz, null);
                    if (t != null) {
                        beanList.add(t);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return beanList;
    }

    /**
     * 将 对象编码为 JSON格式
     *
     * @param t 待封装的对象
     * @return String: 封装后JSONObject String格式
     * @version 1.0
     */
    public static <T> String toJson(T t) {
        if (t == null) {
            return "{}";
        }
        return objectToJson(t);
    }

    public static String createBean(String jsonStr, String className) {
        try {
            JSONObject job = new JSONObject(jsonStr);
            return createObject(job, className, 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }


    private static <T, V> T parseObject(JSONObject job, Class<T> c, V v) {
        T t = null;
        try {
            if (null == v) {
                t = c.newInstance();
            } else {
                Constructor<?> constructor = c.getDeclaredConstructors()[0];
                constructor.setAccessible(true);
                t = (T) constructor.newInstance(v);
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Field[] fields = c.getDeclaredFields();
        Method[] methods = c.getDeclaredMethods();
        List<Method> methodList = new ArrayList<>();
        for (Method method : methods) {
            if (method.getParameterTypes().length == 1 && method.getName().startsWith("set")) {
                methodList.add(method);
            }
        }
        for (Method method : methodList) {
            method.setAccessible(true);
            Class<?> type = method.getParameterTypes()[0];
            String name = getFieldNameFromSet(method.getName());
            if (TextUtils.isEmpty(name)) {
                continue;
            }
            if (!job.has(name)) {
                continue;
            }

            String typeName = type.getName();
            if (typeName.equals("java.lang.String")) {
                try {
                    String value = job.getString(name);
                    if (value != null && value.equals("null")) {
                        value = "";
                    }
                    if (t != null) {
                        method.invoke(t, value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        method.invoke(t, "");
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
            } else if (typeName.equals("int") || typeName.equals("java.lang.Integer")) {
                try {
                    method.invoke(t, job.getInt(name));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (typeName.equals("boolean") || typeName.equals("java.lang.Boolean")) {
                try {
                    method.invoke(t, job.getBoolean(name));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (typeName.equals("float") || typeName.equals("java.lang.Float")) {
                try {
                    method.invoke(t, Float.valueOf(job.getString(name)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (typeName.equals("double") || typeName.equals("java.lang.Double")) {
                try {
                    method.invoke(t, job.getDouble(name));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (typeName.equals("long") || typeName.equals("java.lang.Long")) {
                try {
                    method.invoke(t, job.getLong(name));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (typeName.equals("java.util.List") || typeName.equals("java.util.ArrayList")) {
                try {
                    Object obj = job.get(name);
                    Type genericType = method.getGenericParameterTypes()[0];
                    String className = genericType.toString().replace("<", "")
                            .replace(type.getName(), "").replace(">", "");
                    Class<?> clazz = Class.forName(className);
                    if (obj instanceof JSONArray) {
                        ArrayList<?> objList = parseArray((JSONArray) obj, clazz, t);
                        method.invoke(t, objList);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    Object obj = job.get(name);
                    Class<?> clazz = Class.forName(typeName);
                    if (obj instanceof JSONObject) {
                        Object parseson = parseObject((JSONObject) obj, clazz, null);
                        method.invoke(t, parseson);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }

        return t;
    }

    private static String getFieldNameFromSet(String name) {
        if (name.startsWith("set") && name.length() > 3) {
            name = name.substring(3);
            if (Character.isUpperCase(name.charAt(0))) {
                return new StringBuilder().append(Character.toLowerCase(name.charAt(0))).append(name.substring(1)).toString();
            }
        }
        return "";
    }


    private static <T, V> ArrayList<T> parseArray(JSONArray array, Class<T> c, V v) {
        ArrayList<T> list = new ArrayList<T>(array.length());
        try {
            for (int i = 0; i < array.length(); i++) {
                if (array.get(i) instanceof JSONObject) {
                    T t = parseObject(array.getJSONObject(i), c, null);
                    list.add(t);
                } else {
                    list.add((T) array.get(i));
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }


    private static <T> String objectToJson(T t) {
        Method[] methods = t.getClass().getDeclaredMethods();
        Field[] fields = t.getClass().getDeclaredFields();
        StringBuilder sb = new StringBuilder(fields.length << 4);
        sb.append("{");

        for (Field field : fields) {
            field.setAccessible(true);
            Class<?> type = field.getType();
            String name = field.getName();

            // 'this$Number' 是内部类的外部类引用(指针)字段
            if (name.contains("this$")) {
                continue;
            }

            String typeName = type.getName();
            if (typeName.equals("java.lang.String")) {
                try {
                    sb.append("\"" + name + "\":");
                    sb.append(stringToJson((String) field.get(t)));
                    sb.append(",");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (typeName.equals("boolean") ||
                    typeName.equals("java.lang.Boolean") ||
                    typeName.equals("int") ||
                    typeName.equals("java.lang.Integer") ||
                    typeName.equals("float") ||
                    typeName.equals("java.lang.Float") ||
                    typeName.equals("double") ||
                    typeName.equals("java.lang.Double") ||
                    typeName.equals("long") ||
                    typeName.equals("java.lang.Long")) {
                try {
                    sb.append("\"" + name + "\":");
                    sb.append(field.get(t));
                    sb.append(",");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (typeName.equals("java.util.List") ||
                    typeName.equals("java.util.ArrayList")) {
                try {
                    List<?> objList = (List<?>) field.get(t);
                    if (null != objList && objList.size() > 0) {
                        sb.append("\"" + name + "\":");
                        sb.append("[");
                        String toJson = listToJson((List<?>) field.get(t));
                        sb.append(toJson);
                        sb.setCharAt(sb.length() - 1, ']');
                        sb.append(",");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    sb.append("\"" + name + "\":");
                    sb.append("{");
                    sb.append(objectToJson(field.get(t)));
                    sb.setCharAt(sb.length() - 1, '}');
                    sb.append(",");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
        if (sb.length() == 1) {
            sb.append("}");
        }
        sb.setCharAt(sb.length() - 1, '}');
        return sb.toString();
    }

    /**
     * 将 List 对象编码为 JSON格式
     *
     * @param objList 待封装的对象集合
     * @return String:封装后JSONArray String格式
     * @version 1.0
     * @date 2015-10-11
     * @Author zhou.wenkai
     */
    private static <T> String listToJson(List<T> objList) {
        final StringBuilder sb = new StringBuilder();
        for (T t : objList) {
            if (t instanceof String) {
                sb.append(stringToJson((String) t));
                sb.append(",");
            } else if (t instanceof Boolean ||
                    t instanceof Integer ||
                    t instanceof Float ||
                    t instanceof Double) {
                sb.append(t);
                sb.append(",");
            } else {
                sb.append(objectToJson(t));
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * 将 String 对象编码为 JSON格式，只需处理好特殊字符
     *
     * @param str String 对象
     * @return String:JSON格式
     * @version 1.0
     * @date 2015-10-11
     * @Author zhou.wenkai
     */
    private static String stringToJson(final String str) {
        if (str == null || str.length() == 0) {
            return "\"\"";
        }
        final StringBuilder sb = new StringBuilder(str.length() + 2 << 4);
        sb.append('\"');
        for (int i = 0; i < str.length(); i++) {
            final char c = str.charAt(i);

            sb.append(c == '\"' ? "\\\"": c == '\\' ? "\\\\"
                    : c == '/' ? "\\/": c == '\b' ? "\\b": c == '\f' ? "\\f"
                    : c == '\n' ? "\\n": c == '\r' ? "\\r"
                    : c == '\t' ? "\\t": c);
        }
        sb.append('\"');
        return sb.toString();
    }

    /**
     * 由JSONObject生成Bean对象
     *
     * @param job
     * @param className 待生成Bean对象的名称
     * @param outCount  外部类的个数
     * @return LinkedList<String>: 生成的Bean对象
     * @version 1.0
     * @date 2015-10-16
     * @Author zhou.wenkai
     */
    private static String createObject(JSONObject job, String className, int outCount) {
        final StringBuilder sb = new StringBuilder();
        String separator = System.getProperty("line.separator");

        // 生成的Bean类前部的缩进空间
        String classFrontSpace = "";
        // 生成的Bean类字段前部的缩进空间
        String fieldFrontSpace = "    ";
        for (int i = 0; i < outCount; i++) {
            classFrontSpace += "    ";
            fieldFrontSpace += "    ";
        }

        sb.append(classFrontSpace + "public class " + className + " {");

        Iterator<?> it = job.keys();
        while (it.hasNext()) {
            String key = (String) it.next();
            try {
                Object obj = job.get(key);
                if (obj instanceof JSONArray) {
                    // 判断类是否为基本数据类型,如果为自定义类则字段类型取将key的首字母大写作为内部类名称
                    String fieldType = ((JSONArray) obj).get(0) instanceof JSONObject ?
                            "": ((JSONArray) obj).get(0).getClass().getSimpleName();
                    if (fieldType == "") {
                        fieldType = String.valueOf(Character.isUpperCase(key.charAt(0)) ?
                                key.charAt(0): Character.toUpperCase(key.charAt(0))) + key.substring(1);
                    }
                    sb.append(separator);
                    sb.append(fieldFrontSpace + "public List<" + fieldType + "> " + key + ";");

                    // 如果字段类型为自定义类类型,则取JSONArray中第一个JSONObject生成Bean
                    if (((JSONArray) obj).get(0) instanceof JSONObject) {
                        sb.append(separator);
                        sb.append(separator);
                        sb.append(fieldFrontSpace + "/** " + fieldType + " is the inner class of " + className + " */");
                        sb.append(separator);
                        sb.append(createObject((JSONObject) ((JSONArray) obj).get(0), fieldType, outCount + 1));
                    }
                } else if (obj instanceof JSONObject) {
                    String fieldType = String.valueOf(Character.isUpperCase(key.charAt(0)) ?
                            key.charAt(0): Character.toUpperCase(key.charAt(0))) + key.substring(1);
                    sb.append(separator);
                    sb.append(fieldFrontSpace + "public List<" + fieldType + "> " + key + ";");
                    sb.append(separator);
                    sb.append(separator);
                    sb.append(fieldFrontSpace + "/** " + fieldType + " is the inner class of " + className + " */");
                    sb.append(separator);
                    sb.append(createObject((JSONObject) obj, fieldType, outCount + 1));
                } else {
                    String type = obj.getClass().getSimpleName();
                    sb.append(separator);
                    sb.append(fieldFrontSpace + "public " + type + " " + key + ";");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        sb.append(separator);
        sb.append(classFrontSpace + "}");
        sb.append(separator);

        return sb.toString();
    }

}
