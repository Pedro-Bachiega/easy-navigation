import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import br.com.arch.toolkit.lumber.DebugTree
import br.com.arch.toolkit.lumber.Lumber
import com.pedrobneto.easy.navigation.sample.ui.NavigationSample

fun main() = application {
    Lumber.plant(DebugTree())
    Window(
        onCloseRequest = ::exitApplication,
        title = "Easy Navigation",
        content = { NavigationSample() }
    )
}
