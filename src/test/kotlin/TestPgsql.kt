import com.yuping.balala.ext.initPgsql
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.TestSuite
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.kotlin.core.json.array
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.ext.sql.getConnectionAwait
import io.vertx.kotlin.ext.sql.updateWithParamsAwait
import kotlinx.coroutines.experimental.launch
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(VertxUnitRunner::class)
class TestPgsql {
    @Before
    fun before(context: TestContext) {
    }

    @Test
    fun testInsert(): Unit {
        var suite = TestSuite.create("the_test_pgsql")
        val vertx = Vertx.vertx()
        val initPgsql = vertx.initPgsql()


        initPgsql.getConnection {
            it.result().updateWithParams(
                "INSERT INTO users (autos) VALUES (?::JSON)",
                JsonArray().add(JsonObject().put("a","b"))
            ) {

            }
        }
       /* launch {
            val updateWithParamsAwait = initPgsql.getConnectionAwait().updateWithParamsAwait("INSERT INTO users ('autos', 'info') VALUES (?::JSON,?::JSON)", json {
                array(obj {
                    "b" to "b"
                }, obj { "b" to "bc" })
            })
            print(updateWithParamsAwait)
        }*/
    }
}
