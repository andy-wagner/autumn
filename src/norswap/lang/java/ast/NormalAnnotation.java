package norswap.lang.java.ast;

import com.google.auto.value.AutoValue;
import norswap.utils.Pair;
import java.util.List;

@AutoValue
public abstract class NormalAnnotation implements TAnnotation
{

    public abstract List<Identifier> name();
    public abstract List<Pair<Identifier, AnnotationElement>> elements();

    public static NormalAnnotation make
        (List<Identifier> name, List<Pair<Identifier, AnnotationElement>> elements) {
        return new AutoValue_NormalAnnotation(name, elements);
    }
}
