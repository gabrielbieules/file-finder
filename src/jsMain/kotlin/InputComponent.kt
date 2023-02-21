import kotlinx.css.FontWeight
import kotlinx.css.fontWeight
import kotlinx.css.margin
import react.*
import react.dom.*
import kotlinx.html.js.*
import kotlinx.html.InputType
import org.w3c.dom.events.Event
import org.w3c.dom.HTMLInputElement
import styled.css
import styled.styledDiv
import styled.styledForm
import styled.styledP

external interface InputProps : Props {
    var onSubmit: (String) -> Unit
    var label: String?
    var placeholder: String?
}

val inputComponent = fc<InputProps> { props ->
    val (text, setText) = useState("")

    val submitHandler: (Event) -> Unit = {
        it.preventDefault()
        setText("")
        props.onSubmit(text)
    }

    val changeHandler: (Event) -> Unit = {
        val value = (it.target as HTMLInputElement).value
        setText(value)
    }

    styledForm {
        attrs.onSubmitFunction = submitHandler
        styledDiv {
            css {
                fontWeight = FontWeight.bolder
            }
            +props.label.orEmpty()
        }
        input(InputType.text) {
            attrs.onChangeFunction = changeHandler
            attrs.value = text
            attrs.placeholder = props.placeholder.orEmpty()
        }
    }
}