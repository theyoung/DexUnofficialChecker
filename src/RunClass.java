import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;

import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.reference.DexBackedTypeReference;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class RunClass {
    private static final Map<String, DefinedClass> definedClassesInDex = new HashMap<>();

    public static void main(String[] args) throws Exception{
        File file = new File("dex/apk_name.apk");

        DexBackedDexFile dexFile = DexFileFactory.loadDexEntry(file, "classes.dex", true, Opcodes.getDefault()).getDexFile();

        dexFile.getClasses().stream().forEach(dexBackedClassDef -> {
            DefinedClass dx = DexAnalyzer.definedClassFrom(dexBackedClassDef);
            definedClassesInDex.put(dx.getName(), dx);
        });
        System.out.println(String.valueOf(dexFile.getClasses().size()));

        List list0 = dexFile.getReferences(0); //String Ref
        List list1 = dexFile.getReferences(1); // Type Ref
        List list2 = dexFile.getReferences(2); // Field Ref
        List list3 = dexFile.getReferences(3); // Method Ref

        System.out.println("String Ref" + list0.size());
        System.out.println("Type Ref" + list1.size());
        System.out.println("Field Ref" + list2.size());
        System.out.println("Method Ref" + list3.size());

        List<DexBackedTypeReference> filtered = (List<DexBackedTypeReference>) list1.stream().filter((Predicate<DexBackedTypeReference>) typeRef-> {
            return 0 < typeRef.getType().indexOf("sun");
        }).collect(Collectors.toList());

        System.out.println("Error Count = " + String.valueOf(filtered.size()));
        filtered.forEach((item)->{
            System.out.println(item.getType());
        });

    }
}
