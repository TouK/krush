package pl.touk.krush.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import javax.persistence.AttributeOverride
import javax.persistence.AttributeOverrides
import javax.persistence.JoinColumn
import javax.persistence.JoinColumns
import kotlin.reflect.KClass

typealias KSPropertyWithClassDeclaration = Pair<KSPropertyDeclaration, KSClassDeclaration>

data class AnnotationEnvironment(
    val entities: List<KSClassDeclaration>,
    val ids: List<KSPropertyWithClassDeclaration>,
    val embeddedIds: List<KSPropertyWithClassDeclaration>,
    val columns: List<KSPropertyWithClassDeclaration>,
    val embeddedColumns: List<KSPropertyWithClassDeclaration>,
    val oneToMany: List<KSPropertyWithClassDeclaration>,
    val manyToOne: List<KSPropertyWithClassDeclaration>,
    val manyToMany: List<KSPropertyWithClassDeclaration>,
    val oneToOne: List<KSPropertyWithClassDeclaration>,
    val embeddableColumns: List<KSPropertyWithClassDeclaration> = emptyList()
)

@OptIn(KspExperimental::class)
internal fun <T : Annotation> KSAnnotated.getAnnotationByType(annotationKClass: KClass<T>): T? =
    this.getAnnotationsByType(annotationKClass).firstOrNull()

fun KSAnnotated.getKSAnnotationByType(annotationKClass: KClass<*>): KSAnnotation? =
    this.annotations.firstOrNull {
        it.shortName.getShortName() == annotationKClass.simpleName && it.annotationType.resolve().declaration
            .qualifiedName?.asString() == annotationKClass.qualifiedName
    }


fun KSAnnotated.mappingOverrides(): List<AttributeOverride> {
    return (this.getAnnotationByType(AttributeOverrides::class)?.value?.toList() ?: emptyList()) +
            (this.getAnnotationByType(AttributeOverride::class)?.let { listOf(it) } ?: emptyList())
}

fun KSAnnotated.joinColumns(): List<JoinColumn> {
    return (this.getAnnotationByType(JoinColumns::class)?.value?.toList() ?: emptyList()) +
            (this.getAnnotationByType(JoinColumn::class)?.let { listOf(it) } ?: emptyList())
}