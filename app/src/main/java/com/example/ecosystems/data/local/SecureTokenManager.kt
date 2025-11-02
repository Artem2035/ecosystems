import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import android.util.Base64
import com.example.ecosystems.CryptoConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

class SecureTokenManager (private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
    }

    private val aead by lazy { CryptoConfig.getAead(context) }

    fun saveToken(token: String) {
        val ciphertext = aead.encrypt(token.toByteArray(), null)
        val encoded = Base64.encodeToString(ciphertext, Base64.NO_WRAP)

         runBlocking{
             context.dataStore.edit { prefs ->
                 prefs[TOKEN_KEY] = encoded
             }
         }
    }

    fun loadToken(): String? = runBlocking {
        val prefs = context.dataStore.data.first()
        val encoded = prefs[TOKEN_KEY] ?: return@runBlocking null
        try {
            val decoded = Base64.decode(encoded, Base64.NO_WRAP)
            val decrypted = aead.decrypt(decoded, null)
            String(decrypted)
        } catch (e: Exception) {
            null
        }
    }

    fun clearToken() {
        runBlocking{
            context.dataStore.edit { prefs ->
                prefs.remove(TOKEN_KEY)
            }
        }
    }
}
