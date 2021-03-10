import javax.lang.model.element.VariableElement
import javax.persistence.AttributeOverride
import javax.persistence.AttributeOverrides
import javax.persistence.JoinColumn
import javax.persistence.JoinColumns

fun VariableElement.mappingOverrides(): List<AttributeOverride> {
    return (this.getAnnotation(AttributeOverrides::class.java)?.value?.toList() ?: emptyList()) +
            (this.getAnnotation(AttributeOverride::class.java)?.let { listOf(it) } ?: emptyList())
}

fun VariableElement.joinColumns(): List<JoinColumn> {
    return (this.getAnnotation(JoinColumns::class.java)?.value?.toList() ?: emptyList()) +
            (this.getAnnotation(JoinColumn::class.java)?.let { listOf(it) } ?: emptyList())
}
