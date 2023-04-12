package core.utils;

import arc.util.Reflect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChainReflect {
    // Replace chain of reflect methods with one
    public static void set(Object object, Object value, List<String> pnames) {
        List<String> names = new ArrayList<String>(pnames);
        Object currentObject = object;
        String finalName = names.get(names.size() - 1);
        names.remove(names.size() - 1);

        for (String name : names) {
            currentObject = Reflect.get(currentObject, name);
        }
        Reflect.set(currentObject.getClass(), currentObject, finalName, value);
    }

    public static void set(Object object, Object value, String... names) {
        set(object, value, Arrays.asList(names));
    }
}
