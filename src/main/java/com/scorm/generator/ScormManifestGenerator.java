package com.scorm.generator;

import com.scorm.generator.model.Quiz;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.StringWriter;
import java.util.UUID;

/**
 * Generates SCORM 2004 4th Edition manifest files
 */
public class ScormManifestGenerator {
    
    private static final String SCORM_NAMESPACE = "http://www.imsglobal.org/xsd/imscp_v1p1";
    private static final String ADLCP_NAMESPACE = "http://www.adlnet.org/xsd/adlcp_v1p3";
    private static final String ADLSEQ_NAMESPACE = "http://www.adlnet.org/xsd/adlseq_v1p3";
    private static final String ADLNAV_NAMESPACE = "http://www.adlnet.org/xsd/adlnav_v1p3";
    private static final String IMSSS_NAMESPACE = "http://www.imsglobal.org/xsd/imsss";

    public String generateManifest(Quiz quiz) {
        try {
            Document document = DocumentHelper.createDocument();
            
            // Root manifest element
            Element manifest = document.addElement("manifest");
            manifest.addNamespace("", SCORM_NAMESPACE);
            manifest.addNamespace("adlcp", ADLCP_NAMESPACE);
            manifest.addNamespace("adlseq", ADLSEQ_NAMESPACE);
            manifest.addNamespace("adlnav", ADLNAV_NAMESPACE);
            manifest.addNamespace("imsss", IMSSS_NAMESPACE);
            
            manifest.addAttribute("identifier", "MANIFEST-" + UUID.randomUUID().toString());
            manifest.addAttribute("version", "1.0");
            
            // Metadata
            addMetadata(manifest);
            
            // Organizations
            addOrganizations(manifest, quiz);
            
            // Resources
            addResources(manifest, quiz);
            
            return formatXML(document);
            
        } catch (Exception e) {
            throw new RuntimeException("Error generating SCORM manifest", e);
        }
    }
    
    private void addMetadata(Element manifest) {
        Element metadata = manifest.addElement("metadata");
        metadata.addElement("schema").setText("ADL SCORM");
        metadata.addElement("schemaversion").setText("2004 4th Edition");
    }
    
    private void addOrganizations(Element manifest, Quiz quiz) {
        Element organizations = manifest.addElement("organizations");
        organizations.addAttribute("default", "ORG-" + quiz.getId());
        
        Element organization = organizations.addElement("organization");
        organization.addAttribute("identifier", "ORG-" + quiz.getId());
        organization.addAttribute("adlseq:objectivesGlobalToSystem", "false");
        
        organization.addElement("title").setText(quiz.getTitle());
        
        // Add item for the quiz
        Element item = organization.addElement("item");
        item.addAttribute("identifier", "ITEM-" + quiz.getId());
        item.addAttribute("identifierref", "RES-" + quiz.getId());
        item.addAttribute("isvisible", "true");
        
        item.addElement("title").setText(quiz.getTitle());
        
        // Add ADLCP data
        Element adlcpData = item.addElement("adlcp:data");
        adlcpData.addAttribute("adlcp:map", "");
        
        // Add completion threshold
        Element completionThreshold = item.addElement("adlcp:completionThreshold");
        completionThreshold.addAttribute("adlcp:completedByMeasure", "true");
        completionThreshold.addAttribute("adlcp:minProgressMeasure", "1.0");
    }
    
    private void addResources(Element manifest, Quiz quiz) {
        Element resources = manifest.addElement("resources");
        
        Element resource = resources.addElement("resource");
        resource.addAttribute("identifier", "RES-" + quiz.getId());
        resource.addAttribute("type", "webcontent");
        resource.addAttribute("adlcp:scormType", "sco");
        resource.addAttribute("href", "index.html");
        
        // Add files
        resource.addElement("file").addAttribute("href", "index.html");
        resource.addElement("file").addAttribute("href", "quiz.js");
        resource.addElement("file").addAttribute("href", "scorm_api.js");
        resource.addElement("file").addAttribute("href", "style.css");
    }
    
    private String formatXML(Document document) throws Exception {
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding("UTF-8");
        
        StringWriter stringWriter = new StringWriter();
        XMLWriter xmlWriter = new XMLWriter(stringWriter, format);
        xmlWriter.write(document);
        xmlWriter.close();
        
        return stringWriter.toString();
    }
}
