import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import br.com.arch.toolkit.lumber.DebugOak
import br.com.arch.toolkit.lumber.Lumber
import com.pedrobneto.easy.navigation.sample.ui.NavigationSample

fun main() = application {
    Lumber.plant(DebugOak())
    Window(
        onCloseRequest = ::exitApplication,
        title = "Easy Navigation",
        content = { NavigationSample() }
    )
}
