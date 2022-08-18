package pl.touk.krush.validation

class ConverterTypeNotFoundException(converterType: String) :
        RuntimeException("Could not resolve $converterType converter type") {
}
