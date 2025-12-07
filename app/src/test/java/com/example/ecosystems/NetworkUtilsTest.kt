
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.example.ecosystems.utils.isInternetAvailable
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NetworkUtilsTest {

    private val mockContext = mockk<Context>(relaxed = true)
    private val mockConnectivityManager = mockk<ConnectivityManager>()
    private val mockNetwork = mockk<Network>()
    private val mockCapabilities = mockk<NetworkCapabilities>()

    @Before
    fun setUp() {
        // Когда Context.getSystemService(Context.CONNECTIVITY_SERVICE) вызывается —
        // вернуть наш mock ConnectivityManager
        every { mockContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager
    }

    @After
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun `isInternetAvailable returns true when network and capabilities have INTERNET`() {
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockCapabilities
        every { mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        val result = mockContext.isInternetAvailable()

        assertTrue(result)
        verify { mockConnectivityManager.activeNetwork }
        verify { mockConnectivityManager.getNetworkCapabilities(mockNetwork) }
        verify { mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) }
    }

    @Test
    fun `isInternetAvailable returns false when there is no active network`() {
        every { mockConnectivityManager.activeNetwork } returns null

        val result = mockContext.isInternetAvailable()

        assertFalse(result)
        verify { mockConnectivityManager.activeNetwork }
        // getNetworkCapabilities не вызывается, так как функция вернула раньше
        verify(exactly = 0) { mockConnectivityManager.getNetworkCapabilities(any()) }
    }

    @Test
    fun `isInternetAvailable returns false when capabilities are null`() {
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns null

        val result = mockContext.isInternetAvailable()

        assertFalse(result)
        verify { mockConnectivityManager.activeNetwork }
        verify { mockConnectivityManager.getNetworkCapabilities(mockNetwork) }
    }

    @Test
    fun `isInternetAvailable returns false when capabilities do not have INTERNET`() {
        every { mockConnectivityManager.activeNetwork } returns mockNetwork
        every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockCapabilities
        every { mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns false

        val result = mockContext.isInternetAvailable()

        assertFalse(result)
        verify { mockConnectivityManager.activeNetwork }
        verify { mockConnectivityManager.getNetworkCapabilities(mockNetwork) }
        verify { mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) }
    }
}