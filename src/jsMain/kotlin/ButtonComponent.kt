import kotlinx.css.Cursor
import kotlinx.css.cursor
import kotlinx.css.padding
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.Props
import react.dom.attrs
import react.fc
import styled.css
import styled.styledButton

external interface ButtonProps : Props {
    var onClick: () -> Unit
    var label: String
}

val buttonComponent = fc<ButtonProps> { props ->
    val submitHandler: (Event) -> Unit = {
        it.preventDefault()
        props.onClick()
    }

    styledButton {
        css {
            padding = "8px"
            cursor = Cursor.pointer
        }
        attrs {
            onClickFunction = submitHandler
        }
        +props.label
    }
}