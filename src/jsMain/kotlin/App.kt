import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.css.*
import react.*
import react.dom.*
import styled.css
import styled.styledDiv
import styled.styledH1
import kotlin.js.Date

private val scope = MainScope()

val app = fc<Props> {
    var locations by useState(emptyList<Location>())
    var searchResult by useState(SearchResult(emptyList(), 0))
    var pageIndex by useState(0)
    var serviceStatus by useState(ServiceStatus(null, false, 0))
    var isLoading by useState(true)

    var text: String by useState("")
    var minSize: String? by useState(null)
    var maxSize: String? by useState(null)

    useEffectOnce {
        scope.launch {
            locations = getLocations()
        }
    }

    useEffect(locations, pageIndex) {
        scope.launch {
            searchResult = searchEvidences(text, minSize, maxSize, pageIndex, 50)
            isLoading = false
        }
    }

    useEffect(locations) {
        scope.launch {
            serviceStatus = getServiceStatus()
        }
    }

    val refresh = {
        scope.launch {
            pageIndex = 0
            locations = getLocations()
            searchResult = searchEvidences(text, minSize, maxSize, 0, 50)
            serviceStatus = getServiceStatus()
        }
    }


    styledDiv {
        css {
            display = Display.flex
            alignItems = Align.center
            justifyContent = JustifyContent.spaceBetween
        }

        styledH1 {
            css {
                color = Color("#006DCC")
            }
            +"Locations List"
        }

        styledDiv {
            css {
                flex(1.0, 1.0, FlexBasis.auto)
                display = Display.flex
                justifyContent = JustifyContent.flexEnd
            }

            styledDiv {
                css {
                    margin = "0 8px"
                }
                child(buttonComponent) {
                    attrs {
                        onClick = {
                            scope.launch {
                                refresh()
                            }
                        }
                        label = "Refresh Page"
                    }
                }
            }

            styledDiv {
                css {
                    margin = "0 8px"
                }
                child(buttonComponent) {
                    attrs {
                        onClick = {
                            scope.launch {
                                refreshEvidences()
                                pageIndex = 0
                            }
                        }
                        label = "Rescan Locations"
                    }
                }
            }

            styledDiv {
                css {
                    margin = "0 8px"
                }
                child(buttonComponent) {
                    attrs.onClick = {
                        scope.launch {
                            saveSnapshot()
                        }
                    }
                    attrs.label = "Save Snapshot"
                }
            }

            styledDiv {
                css {
                    margin = "0 8px"
                }
                child(buttonComponent) {
                    attrs.onClick = {
                        scope.launch {
                            loadSnapshot()
                            refresh()
                        }
                    }
                    attrs.label = "Load Snapshot"
                }
            }

            styledDiv {
                css {
                    margin = "0 8px"
                }
                styledDiv {
                    +"Scanning: ${serviceStatus.isScanning}"
                }
                serviceStatus.lastScanned?.let { lastScanned ->
                    //TODO: use kotlinx-datetime
                    val date = Date(lastScanned)
                    styledDiv {
                        +"Last scanned: ${date.toISOString()}"
                    }
                }
                styledDiv {
                    +"Files: ${serviceStatus.filesNumber}"
                }
            }
        }
    }

    styledDiv {
        child(inputComponent) {
            attrs.label = "Add location"
            attrs.onSubmit = { input ->
                scope.launch {
                    addLocation(Location(input))
                    locations = getLocations()
                    pageIndex = 0
                }
            }
        }
    }

    styledDiv {
        css {
            padding = "16px 0"
        }
        locations.sortedByDescending(Location::filePath).forEach { location ->
            key = location.toString()

            styledDiv {
                css {
                    padding = "4px 0"
                    display = Display.flex
                }

                styledDiv {
                    css {
                        margin = "0 16px 0 0"
                    }
                    child(buttonComponent) {
                        attrs.onClick = {
                            scope.launch {
                                deleteLocation(location)
                                locations = getLocations()
                                pageIndex = 0
                            }
                        }
                        attrs.label = "Remove"
                    }
                }

                styledDiv {
                    css {
                        margin = "auto 0"
                    }
                    +location.filePath
                }
            }
        }
    }

    styledH1 {
        css {
            color = Color("#006DCC")
        }
        +"Evidences List"
    }

    styledDiv {
        css {
            margin = "16px 0"
        }
        child(filtersComponent) {
            attrs.label = "Evidences filters"
            attrs.onSubmit = { newText, newMinSize, newMaxSize ->
                pageIndex = 0
                scope.launch {
                    searchResult = searchEvidences(newText, newMinSize, newMaxSize, 0, 50)
                }
                text = newText
                minSize = newMinSize
                maxSize = newMaxSize
            }
        }
    }

    styledDiv {
        css {
            margin = "16px 0"
        }
        +"Total: ${searchResult.total} file(s)"
    }

    if (isLoading) {
        p {
            +"Loading..."
        }
    }

    styledDiv {
        css {
            padding = "16px 0"
            display = Display.grid
            gridTemplate = GridTemplate("'1fr 1fr 1fr 1fr'")
            columnGap = LinearDimension("8px")
            rowGap = LinearDimension("4px")
        }

        searchResult.evidences.sortedByDescending(Evidence::fileName).forEach { evidence ->
            key = evidence.toString()

            styledDiv {
                styledDiv {
                    css {
                        textOverflow = TextOverflow.ellipsis
                        maxWidth = LinearDimension("fit-content")
                    }
                    +evidence.fileName
                }
                styledDiv {
                    css {
                        display = Display.flex
                        minHeight = LinearDimension("12px")
                        fontSize = LinearDimension("12px")
                        fontWeight = FontWeight.bold
                    }
                    evidence.tags.forEach { tag ->
                        styledDiv {
                            css {
                                margin = "0 4px"

                                padding = "2px 8px"
                                border = "1px solid #0DA371"
                                borderRadius = LinearDimension("8px")

                                display = Display.flex
                                justifyContent = JustifyContent.spaceBetween
                            }
                            styledDiv {
                                css {
                                    color = Color("#0DA371")
                                }
                                +tag
                            }
                            styledDiv {
                                css {
                                    cursor = Cursor.pointer
                                    padding = "0 0 0 4px"
                                    color = Color("#F57C2C")
                                }
                                attrs.onClick = {
                                    scope.launch {
                                        deleteTag(evidence, tag)
                                        searchResult = searchEvidences(text, minSize, maxSize, 0, (pageIndex + 1) * 50)
                                    }
                                }
                                +"X"
                            }
                        }
                    }
                }
            }
            styledDiv {
                child(inputComponent) {
                    attrs.placeholder = "Add tag"
                    attrs.onSubmit = { tag ->
                        scope.launch {
                            addTag(evidence, tag)
                            searchResult = searchEvidences(text, minSize, maxSize, 0, (pageIndex + 1) * 50)
                        }
                    }
                }
            }
            styledDiv {
                css {
                    textAlign = TextAlign.right
                }
                +"${evidence.fileSize} B"
            }
            styledDiv {
                css {
                    overflowWrap = OverflowWrap.anywhere
                }
                a {
                    attrs.href = "./evidences/${evidence.id}"
                    +evidence.filePath
                }
            }
        }
    }

    styledDiv {
        button {
            attrs.onClick = {
                pageIndex++
            }
            +"Load more"
        }
    }
}