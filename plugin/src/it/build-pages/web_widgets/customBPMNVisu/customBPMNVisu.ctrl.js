function ($scope) {

    //const bpmnContainerElt = document.getElementById('bpmn-container');
    let elementConfiguration = {
        container: 'bpmn-container',
        navigation: { enabled: true } // allow to navigate the diagram with the mouse
    }
    const bpmnVisualization = new bpmnvisu.BpmnVisualization(elementConfiguration);

    const styleSheet = bpmnVisualization.graph.getStylesheet(); // mxStylesheet


    /*********************************
     ************ THEME **************
     *********************************/

    // COLORS
    const defaultStrokeColor = '#414666';
    const defaultFontColor = '#414666';
    const backgroundColor = '#ede7e1';

    const flowNodeColor = '#666666';
    const endEventFillColor = 'pink';
    const endEventStrokeColor = 'FireBrick';
    const startEventFillColor = 'DarkSeaGreen';
    const startEventStrokeColor = 'DarkGreen';
    const taskFillColor = '#dadce8';
    const laneFillColor = '#d4c3b2';
    const poolFillColor = '#d1b9a1';
    const catchAndThrowEventStrokeColor = '#377f87';

    // EVENTS
    bpmnvisu.ShapeUtil.eventKinds().forEach(kind => {
        var fillColor;
        var strokeColor;
        switch (kind) {
            case ('endEvent'):
                fillColor = endEventFillColor;
                strokeColor = endEventStrokeColor;
                break;
            case ('startEvent'):
                fillColor = startEventFillColor;
                strokeColor = startEventStrokeColor;
                break;
            case ('intermediateCatchEvent'):
            case ('intermediateThrowEvent'):
            case ('boundaryEvent'):
                fillColor = backgroundColor;
                strokeColor = catchAndThrowEventStrokeColor;
                break;
            default:
                fillColor = backgroundColor;
                strokeColor = defaultStrokeColor;
                break;
        }
        const style = styleSheet.styles[kind];
        style['fillColor'] = fillColor;
        style['strokeColor'] = strokeColor;
    });

    // TASKS
    bpmnvisu.ShapeUtil.taskKinds().forEach(kind => {
        const style = styleSheet.styles[kind];
        style['fillColor'] = taskFillColor;
        style['fontColor'] = defaultFontColor;
    });

    // CALL ACTIVITIES
    const callActivityStyle = styleSheet.styles[bpmnvisu.ShapeBpmnElementKind.CALL_ACTIVITY];
    callActivityStyle['fillColor'] = taskFillColor;
    callActivityStyle['fontColor'] = defaultFontColor;

    // POOL
    const poolStyle = styleSheet.styles[bpmnvisu.ShapeBpmnElementKind.POOL];
    poolStyle['fillColor'] = poolFillColor;
    poolStyle['swimlaneFillColor'] = backgroundColor;

    // LANE
    const laneStyle = styleSheet.styles[bpmnvisu.ShapeBpmnElementKind.LANE];
    laneStyle['fillColor'] = laneFillColor;

    const defaultVertexStyle = styleSheet.getDefaultVertexStyle();
    defaultVertexStyle['fontColor'] = defaultFontColor;
    defaultVertexStyle['fillColor'] = backgroundColor;
    defaultVertexStyle['strokeColor'] = defaultStrokeColor;

    const defaultEdgeStyle = styleSheet.getDefaultEdgeStyle();
    defaultEdgeStyle['fontColor'] = defaultFontColor;
    defaultEdgeStyle['fillColor'] = backgroundColor;
    defaultEdgeStyle['strokeColor'] = flowNodeColor;

    /**********************************/


    if ($scope.properties.processId && $scope.properties.caseIds) {
        let bpmnContent = loadBpmnFromUrl(diagramUrl($scope.properties.processId))
    }

    function fetchBpmnContent(url) {
        console.log('Fetching BPMN content from url ' + url);
        return fetch(url).then(response => {
            if (!response.ok) {
                throw Error(String(response.status));
            }
            return response.text();
        });
    }
    function fetchCaseInfo(url) {
        console.log('Fetching Case Info from url ' + url);
        return fetch(url).then(response => {
            if (!response.ok) {
                throw Error(String(response.status));
            }
            return response.json();
        });
    }
    function caseInfoUrl(caseId) {
        return '../API/bpm/caseInfo/' + caseId;
    }

    function diagramUrl(processId) {
        return '../API/bpm/diagram/' + processId;
    }

    function loadCaseInfo(url) {
        fetchCaseInfo(url)
            .catch(error => {
                const errorMessage = 'Unable to fetch ' + url + '.' + error;
                throw new Error(errorMessage);
            })
            .then(responseBody => {
                console.log('Case info fetched');
                console.log(responseBody);
                return responseBody;
            })
            .then(caseInfo => {
                Object.keys(caseInfo.flowNodeStatesCounters).forEach(function (key) {
                    if (caseInfo.flowNodeStatesCounters[key].ready || caseInfo.flowNodeStatesCounters[key].waiting) {
                        highlightElement(key);
                        console.log('Highlighted ' + key);
                    } else if (caseInfo.flowNodeStatesCounters[key].completed) {
                        highlightCompletedElement(key);
                    }
                });
                console.log('Case info loaded into Bpmn loaded');
            })
    }

    function loadBpmnFromUrl(url) {
        fetchBpmnContent(url)
            .catch(error => {
                const errorMessage = 'Unable to fetch ' + url + '.' + error;
                throw new Error(errorMessage);
            })
            .then(responseBody => {
                console.log('BPMN content fetched');
                return responseBody;
            })
            .then(bpmn => {
                loadBpmn(bpmn);
                console.log('Bpmn loaded from url ' + url);
            })
            .then(init => {
                $scope.properties.caseIds.filter(elt => elt !== null).forEach(elt => loadCaseInfo(caseInfoUrl(elt)));
            });
    }
    function loadBpmn(bpmnContent) {
        bpmnVisualization.load(bpmnContent, { fit: { type: bpmnvisu.FitType.Horizontal, margin: 10 } });
        console.log()
    }

    function highlightElement(elementName) {
        const elementIds = findIdTheUgliestWayPossible(elementName);
        console.log("Element to highlight", elementName, elementIds);
        /*
        const bpmnElements = bpmnVisualization.bpmnElementsRegistry.getElementsByIds(elementIds);
        const htmlElement = bpmnElements[0].htmlElement;
        htmlElement.classList.toggle('bpmn-activity-success');
        */
        bpmnVisualization.bpmnElementsRegistry.addCssClasses(elementIds, 'bpmn-activity-success');
    }

    function highlightCompletedElement(elementName) {
        const elementIds = findIdTheUgliestWayPossible(elementName);
        console.log("Element completed to highlight", elementName, elementIds);
        bpmnVisualization.bpmnElementsRegistry.addCssClasses(elementIds, 'bpmn-activity-completed');
    }

    function findIdTheUgliestWayPossible(elementName) {
        let ids = [];
        Object.values(bpmnVisualization.graph.model.cells).filter(cell => cell.value === elementName).forEach(cell => ids.push(cell.id))
        return ids;
    }

}