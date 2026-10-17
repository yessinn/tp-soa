package tp;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * TP XML : validation XML/XSD (JAXP) et interrogation XPath.
 * Domaine : gestion d'un cinéma (films, salles, utilisateurs, séances).
 */
public class Main {

    private static final String XML_FILE = "/cinema.xml";
    private static final String XML_INVALIDE = "/cinema-invalide.xml";
    private static final String XSD_FILE = "/cinema.xsd";

    public static void main(String[] args) throws Exception {
        System.out.println("=== 1. VALIDATION XML / XSD ===");
        validerXml(XML_FILE);
        validerXml(XML_INVALIDE);   // volontairement invalide, pour la démonstration

        System.out.println("\n=== 2. INTERROGATION XPATH ===");
        interrogerXPath();
    }

    // ------------------------------------------------------------------
    // Fonctionnalité 1 : validation XML/XSD
    // ------------------------------------------------------------------
    static boolean validerXml(String xmlResource) {
        System.out.println("\nValidation de " + xmlResource + " ...");
        List<String> erreurs = new ArrayList<>();

        try (InputStream xsd = Main.class.getResourceAsStream(XSD_FILE);
             InputStream xml = Main.class.getResourceAsStream(xmlResource)) {

            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new StreamSource(xsd));
            Validator validator = schema.newValidator();

            // On collecte toutes les erreurs au lieu de s'arrêter à la première
            validator.setErrorHandler(new ErrorHandler() {
                public void warning(SAXParseException e)    { erreurs.add("[WARN] " + format(e)); }
                public void error(SAXParseException e)      { erreurs.add("[ERREUR] " + format(e)); }
                public void fatalError(SAXParseException e) throws SAXException { throw e; }
            });

            validator.validate(new StreamSource(xml));
        } catch (SAXException e) {
            erreurs.add("[FATAL] " + e.getMessage());
        } catch (Exception e) {
            erreurs.add("[IO] " + e.getMessage());
        }

        if (erreurs.isEmpty()) {
            System.out.println("  -> Document VALIDE");
            return true;
        }
        System.out.println("  -> Document INVALIDE (" + erreurs.size() + " erreur(s)) :");
        erreurs.forEach(e -> System.out.println("     " + e));
        return false;
    }

    private static String format(SAXParseException e) {
        return "ligne " + e.getLineNumber() + " : " + e.getMessage();
    }

    // ------------------------------------------------------------------
    // Fonctionnalité 2 : interrogation XPath
    // ------------------------------------------------------------------
    static void interrogerXPath() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc;
        try (InputStream xml = Main.class.getResourceAsStream(XML_FILE)) {
            doc = builder.parse(xml);
        }

        XPath xpath = XPathFactory.newInstance().newXPath();

        afficher("Tous les titres de films", doc, xpath, "//film/titre");
        afficher("Utilisateurs actifs", doc, xpath, "//utilisateur[@actif='true']/nom");
        afficher("Séances en promotion (id)", doc, xpath, "//seance[@promotion='true']/@id");
        afficher("Films de science-fiction notés > 8.6", doc, xpath,
                "//film[genre='Science-fiction' and note > 8.6]/titre");
        afficher("Films projetés en promotion", doc, xpath,
                "//film[@id = //seance[@promotion='true']/@filmRef]/titre");
        afficher("Films à venir (statut = prochainement)", doc, xpath,
                "//film[@statut='prochainement']/titre");

        // Expressions retournant un nombre : XPathConstants.NUMBER
        XPathExpression nbFilms = xpath.compile("count(//film)");
        double n = (Double) nbFilms.evaluate(doc, XPathConstants.NUMBER);
        System.out.println("\nNombre de films : " + (int) n);

        XPathExpression prixMoyen = xpath.compile("sum(//seance/prix) div count(//seance)");
        double p = (Double) prixMoyen.evaluate(doc, XPathConstants.NUMBER);
        System.out.printf("Prix moyen d'une séance : %.2f TND%n", p);
    }

    private static void afficher(String titre, Document doc, XPath xpath, String expression)
            throws Exception {
        XPathExpression expr = xpath.compile(expression);
        NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

        System.out.println("\n" + titre + "  [" + expression + "]");
        if (nodes.getLength() == 0) {
            System.out.println("  (aucun résultat)");
        }
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            System.out.println("  - " + node.getTextContent().trim());
        }
    }
}
