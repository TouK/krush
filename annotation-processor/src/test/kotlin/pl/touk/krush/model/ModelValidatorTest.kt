package pl.touk.krush.model

import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import pl.touk.krush.AnnotationProcessorTest
import pl.touk.krush.model.ValidationResult.Error
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

@KotlinPoetMetadataPreview
class ModelValidatorTest(types: Types, elements: Elements) : AnnotationProcessorTest(types, elements), EntityGraphSampleData {

    @Test
    fun shouldNotPassValidationWhenEntityIsNotDataClass() {

        //given
        val validator = DataClassValidator()
        val invalidClassTypeEntity = invalidClassTypeEntity(getTypeEnv())

        //when
        val validationResult = validator.validate(invalidClassTypeEntity)

        //then
        val exceptedErrorMsg = ValidationErrorMessage("Entity ${invalidClassTypeEntity.qualifiedName} is not data class")
        assertThat(validationResult).isExactlyInstanceOf(Error::class.java)
        assertThat((validationResult as Error).errors).containsOnly(exceptedErrorMsg)
    }

    @Test
    fun shouldNotPassValidationWhenEntityIdIsNotPresent() {

        //given
        val validator = EntityIdValidator()
        val idNotPresentEntityDef = idNotPresentEntityDefinition(getTypeEnv())

        //when
        val validationResult = validator.validate(idNotPresentEntityDef)

        //then
        val exceptedErrorMsg = ValidationErrorMessage("No id field specified for entity ${idNotPresentEntityDef.qualifiedName}")
        assertThat(validationResult).isExactlyInstanceOf(Error::class.java)
        assertThat((validationResult as Error).errors).containsOnly(exceptedErrorMsg)
    }

    @Test
    fun shouldNotPassValidationWhenEntityIdTypeIsUnsupported() {

        //given
        val validator = EntityIdTypeValidator()
        val idTypeUnsupportedEntityDef = idTypeUnsupportedEntityDefinition(getTypeEnv())

        //when
        val validationResult = validator.validate(idTypeUnsupportedEntityDef)

        //then
        val exceptedErrorMsg = ValidationErrorMessage("Entity ${idTypeUnsupportedEntityDef.qualifiedName} id type ${idTypeUnsupportedEntityDef.id!!.type} is unsupported. Use property converter instead.")
        assertThat(validationResult).isExactlyInstanceOf(Error::class.java)
        assertThat((validationResult as Error).errors).containsOnly(exceptedErrorMsg)
    }

    @Test
    fun shouldNotPassValidationWhenEntityPropertyTypeIsUnsupported() {

        //given
        val validator = EntityPropertyTypeValidator()
        val propertyTypeUnsupportedEntityDef = propertyTypeUnsupportedEntityDefinition(getTypeEnv())

        //when
        val validationResult = validator.validate(propertyTypeUnsupportedEntityDef)

        //then
        val exceptedErrorMsg = ValidationErrorMessage("Entity ${propertyTypeUnsupportedEntityDef.qualifiedName} has unsupported property type ${propertyTypeUnsupportedEntityDef.properties[0].type}")
        assertThat(validationResult).isExactlyInstanceOf(Error::class.java)
        assertThat((validationResult as Error).errors).containsOnly(exceptedErrorMsg)
    }
}
