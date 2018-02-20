import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.afterburner.AfterburnerModule
import com.fasterxml.jackson.module.kotlin.KotlinModule

val OBJECT_MAPPER = ObjectMapper().apply {
    registerModule(KotlinModule())
    registerModule(AfterburnerModule())
}