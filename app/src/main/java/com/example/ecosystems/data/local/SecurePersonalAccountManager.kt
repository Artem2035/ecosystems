import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.ecosystems.CryptoConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

class SecurePersonalAccountManager(private val context: Context) {

    companion object {
        private val KEY_DATA = stringPreferencesKey("personal_account_data")
    }
    private val aead by lazy { CryptoConfig.getAead(context) }

    // Сохраняем Map<String, Any?> в DataStore (зашифрованным JSON)
    fun saveData(data: MutableMap<String, Any?>) {
        val json = JSONObject(data as Map<*, *>).toString()
        val encrypted = aead.encrypt(json.toByteArray(), null)
        val encryptedBase64 = Base64.encodeToString(encrypted, Base64.DEFAULT)

        runBlocking {
            context.dataStore.edit { prefs ->
                prefs[KEY_DATA] = encryptedBase64
            }
        }
    }

    // Загружаем данные обратно в Map<String, Any?>
    fun loadData(): MutableMap<String, Any?> {
        return runBlocking {
            val prefs = context.dataStore.data.first()
            val encryptedBase64 = prefs[KEY_DATA] ?: return@runBlocking mutableMapOf()

            val encrypted = Base64.decode(encryptedBase64, Base64.DEFAULT)
            val decrypted = aead.decrypt(encrypted, null).decodeToString()

            val json = JSONObject(decrypted)
            val map = mutableMapOf<String, Any?>()
            json.keys().forEach { key ->
                map[key] = json.get(key)
            }
            map
        }
    }

    // Очистка всех данных
    fun clear() {
        runBlocking {
            context.dataStore.edit { prefs ->
                prefs.clear()
            }
        }
    }
}
