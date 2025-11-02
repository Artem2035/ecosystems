import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

// Создаём DataStore как extension property для Context
val Context.dataStore by preferencesDataStore(name = "secure_store")
