import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.afterburner.AfterburnerModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.vertx.core.buffer.Buffer
import java.nio.charset.Charset

val OBJECT_MAPPER = ObjectMapper().apply {
    registerModule(KotlinModule())
    registerModule(AfterburnerModule())
    setSerializationInclusion(JsonInclude.Include.NON_NULL)
    setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
}

inline fun <reified T:Any> Buffer.toObject(): T {
    return OBJECT_MAPPER.readValue(this.toString(Charset.forName("UTF-8")), T::class.java)
}