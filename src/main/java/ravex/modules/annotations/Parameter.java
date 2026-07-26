package ravex.modules.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Parameter {
    String name();
    double min() default 0;
    double max() default 100;
    double step() default 1;
    String[] modes() default {};
    String[] options() default {};
    boolean color() default false;
}
