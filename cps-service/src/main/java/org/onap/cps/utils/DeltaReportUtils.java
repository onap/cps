package org.onap.cps.utils;

import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.api.model.DeltaReport;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.onap.cps.utils.XmlUtils.getDocumentBuilderFactory;
import static org.onap.cps.utils.XmlUtils.getTransformerFactory;

public class DeltaReportUtils {
    private static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        return getDocumentBuilderFactory().newDocumentBuilder();
    }

    /**
     * Converts a collection of DeltaReport objects to XML format using DOM.
     *
     * @param deltaReports The collection of DeltaReport objects.
     * @return The XML string representation of the delta reports.
     */
    public static String buildXmlUsingDom(final List<DeltaReport> deltaReports) {
        try {
            final DocumentBuilder documentBuilder = getDocumentBuilder();
            final Document document = documentBuilder.newDocument();

            final Element rootElement = document.createElement("deltaReports");
            document.appendChild(rootElement);

            int id = 1;
            for (final DeltaReport deltaReport : deltaReports) {
                final Element deltaReportElement = document.createElement("deltaReport");
                deltaReportElement.setAttribute("id", String.valueOf(id++));

                createKeyValueElement(document, deltaReportElement, "action", deltaReport.getAction());
                createKeyValueElement(document, deltaReportElement, "xpath", deltaReport.getXpath());

                convertMapToElement(document, deltaReportElement, "source-data", deltaReport.getSourceData());
                convertMapToElement(document, deltaReportElement, "target-data", deltaReport.getTargetData());

                rootElement.appendChild(deltaReportElement);
            }
            return transformDocumentToString(document);

        } catch (DOMException | ParserConfigurationException | TransformerException exception) {
            throw new DataValidationException("Data Validation Failed", "Failed to build XML deltaReport: " + exception.getMessage(), exception);
        }
    }

    private static void convertMapToElement(final Document document, final Element parentElement, final String name, final Map<String, Serializable> data) {

        if (data != null && !data.isEmpty()) {
            final Element element = document.createElement(name);

            for (final Map.Entry<String, Serializable> mapentry : data.entrySet()) {
                final Element childElement = createXmlElement(document, mapentry);
                element.appendChild(childElement);
            }
            parentElement.appendChild(element);
        }
    }

    private static Element createKeyValueElement(final Document document, final Element parentElement, final String name, final String value) {
        if (value != null && !value.isEmpty()) {
            final Element element = document.createElement(name);
            element.appendChild(document.createTextNode(value));
            parentElement.appendChild(element);
            return element;
        }
        return null;
    }

    private static Element createXmlElement(final Document document, final Map.Entry<String, Serializable> inputMap) {
        final Element element = document.createElement(inputMap.getKey());
        final Serializable mapValue = inputMap.getValue();
        if (mapValue instanceof Collection) {
            final String collectionAsCsvString = ((Collection<?>) mapValue).stream().map(Object::toString).collect(Collectors.joining(", "));
            element.appendChild(document.createTextNode(collectionAsCsvString));
        } else {
            element.appendChild(document.createTextNode(mapValue.toString()));
        }
        return element;
    }

    @SuppressWarnings("SameReturnValue")
    private static String transformDocumentToString(final Document document) throws TransformerException {
        final Transformer transformer = getTransformerFactory().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        final StringWriter stringWriter = new StringWriter();
        final StreamResult streamResult = new StreamResult(stringWriter);
        transformer.transform(new DOMSource(document), streamResult);
        return stringWriter.toString();
    }

}
