import kotlinx.css.*
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onKeyPressFunction
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.Element
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import react.Props
import react.dom.events.KeyboardEvent
import react.dom.input
import react.fc
import react.useState
import styled.css
import styled.styledDiv
import styled.styledForm
import styled.styledP

external interface FiltersProps : Props {
    var onSubmit: (text: String, minSize: String, maxSize: String) -> Unit
    var label: String
    var text: String?
    var minSize: String?
    var maxSize: String?
}

val filtersComponent = fc<FiltersProps> { props ->
    val (text, setText) = useState(props.text.orEmpty())
    var minSize: String? by useState(props.minSize)
    var maxSize: String? by useState(props.maxSize)

    val submitHandler: (Event) -> Unit = {
        it.preventDefault()
        setText("")
        props.onSubmit(text, minSize.orEmpty(), maxSize.orEmpty())
    }
    val keyPressHandler: (Event) -> Unit = { e ->
        val keyboardEvent = e.unsafeCast<KeyboardEvent<Element>>()
        if (keyboardEvent.key == "Enter") {
            e.preventDefault()
            props.onSubmit(text, minSize.orEmpty(), maxSize.orEmpty())
        }
    }

    val changeTextHandler: (Event) -> Unit = {
        val value = (it.target as HTMLInputElement).value
        setText(value)
    }
    val changeMinHandler: (Event) -> Unit = {
        val value = (it.target as HTMLInputElement).value
        minSize = value
    }
    val changeMaxHandler: (Event) -> Unit = {
        val value = (it.target as HTMLInputElement).value
        maxSize = value
    }

    styledForm {
        attrs.onSubmitFunction = submitHandler
        attrs.onKeyPressFunction = keyPressHandler

        styledDiv {
            css {
                fontWeight = FontWeight.bolder
            }
            +props.label
        }
        styledDiv {
            css {
                display = Display.flex
            }
            styledDiv {
                css {
                    margin = "0 8px"
                }
                styledP {
                    css {
                        fontWeight = FontWeight.bold
                        margin = "8px 0 0 0"
                    }
                    +"Search"
                }
                input(InputType.text) {
                    attrs.onChangeFunction = changeTextHandler
                    attrs.value = text
                }
            }
            styledDiv {
                css {
                    margin = "0 8px"
                }
                styledP {
                    css {
                        fontWeight = FontWeight.bold
                        margin = "8px 0 0 0"
                    }
                    +"Min size (in Bytes)"
                }
                input(InputType.number) {
                    attrs.onChangeFunction = changeMinHandler
                    attrs.value = minSize.orEmpty()
                }
            }
            styledDiv {
                css {
                    margin = "0 8px"
                }
                styledP {
                    css {
                        fontWeight = FontWeight.bold
                        margin = "8px 0 0 0"
                    }
                    +"Max size (in Bytes)"
                }
                input(InputType.number) {
                    attrs.onChangeFunction = changeMaxHandler
                    attrs.value = maxSize.orEmpty()
                }
            }
        }
    }
}