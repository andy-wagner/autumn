package norswap.lang.java8
import norswap.lang.java8.resolution.install_java8_resolution_rules
import norswap.lang.java8.typing.install_java8_typing_rules
import norswap.uranium.Reactor

// -------------------------------------------------------------------------------------------------

fun Reactor.install_java8_rules()
{
    attachment = Java8Config()
    val java_virtal_node = JavaVirtualNode()
    roots.add(java_virtal_node)
    install_java8_typing_rules()
    install_java8_resolution_rules()
}

// -------------------------------------------------------------------------------------------------