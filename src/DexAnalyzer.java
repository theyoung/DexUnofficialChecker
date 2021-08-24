import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.DexFileFactory.DexFileNotFoundException;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexBackedField;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.jf.dexlib2.dexbacked.reference.DexBackedFieldReference;
import org.jf.dexlib2.dexbacked.reference.DexBackedMethodReference;
import org.jf.dexlib2.dexbacked.reference.DexBackedTypeReference;

public class DexAnalyzer {
    //primitive Types 정의
    private static Map<Character, String> primitiveTypes = new HashMap<>();
    static {
        primitiveTypes.put('Z', "boolean");
        primitiveTypes.put('B', "byte");
        primitiveTypes.put('C', "char");
        primitiveTypes.put('S', "short");
        primitiveTypes.put('I', "int");
        primitiveTypes.put('J', "long");
        primitiveTypes.put('F', "float");
        primitiveTypes.put('D', "double");
        primitiveTypes.put('V', "void");
    }
    
    public static DefinedClass definedClassFrom(DexBackedClassDef def) {
        String name = toCanonicalName(def.getType());
        String superClassName = toCanonicalName(def.getSuperclass());
        Collection<String> interfaceNames = def.getInterfaces().stream()
                .map(n -> toCanonicalName(n))
                .collect(Collectors.toList());

        Collection<DefinedMethod> methods = StreamSupport
                .stream(def.getMethods().spliterator(), false /* parallel */)
                .map(DexAnalyzer::definedMethodFrom).collect(Collectors.toList());

        Collection<DefinedField> fieldsStatic = StreamSupport
                .stream(def.getStaticFields().spliterator(), false /* parallel */)
                .map(DexAnalyzer::definedFieldFrom).collect(Collectors.toList());

        Collection<DefinedField> fieldsInstance = StreamSupport
                .stream(def.getInstanceFields().spliterator(), false /* parallel */)
                .map(DexAnalyzer::definedFieldFrom).collect(Collectors.toList());

        fieldsStatic.addAll(fieldsInstance);

        return new DefinedClass(name, superClassName, interfaceNames, methods, fieldsStatic);
    }

    private static DefinedField definedFieldFrom(DexBackedField def) {
        String signature = def.getName() + ":" + toCanonicalName(def.getType());
        String definingClass = toCanonicalName(def.getDefiningClass());
        return new DefinedField(signature, definingClass);
    }
    private static DefinedMethod definedMethodFrom(DexBackedMethod def) {
        StringBuffer sb = new StringBuffer();
        sb.append(def.getName());
        sb.append('(');
        StringJoiner joiner = new StringJoiner(",");
        for (String param : def.getParameterTypes()) {
            joiner.add(toCanonicalName(param));
        }
        sb.append(joiner.toString());
        sb.append(')');
        final boolean isConstructor = def.getName().equals("<init>");
        if (!isConstructor) {
            sb.append(toCanonicalName(def.getReturnType()));
        }

        String signature = sb.toString();
        String definingClass = toCanonicalName(def.getDefiningClass());
        return new DefinedMethod(signature, definingClass);
    }

    // [[Lcom/foo/bar/MyClass$Inner; becomes
    // com.foo.bar.MyClass.Inner[][]
    // and [[I becomes int[][]
    private static String toCanonicalName(String name) {
        int arrayDepth = 0;
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) == '[') {
                arrayDepth++;
            } else {
                break;
            }
        }

        // test the first character.
        final char firstChar = name.charAt(arrayDepth);
        if (primitiveTypes.containsKey(firstChar)) {
            name = primitiveTypes.get(firstChar);
        } else if (firstChar == 'L') {
            // omit the leading 'L' and the trailing ';'
            name = name.substring(arrayDepth + 1, name.length() - 1);

            // replace '/' and '$' to '.'
            name = name.replace('/', '.').replace('$', '.');
        } else {
            throw new RuntimeException("Invalid type name " + name);
        }

        // add []'s, if any
        if (arrayDepth > 0) {
            for (int i = 0; i < arrayDepth; i++) {
                name += "[]";
            }
        }
        return name;
    }
}
