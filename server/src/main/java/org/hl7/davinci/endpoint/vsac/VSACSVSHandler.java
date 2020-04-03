package org.hl7.davinci.endpoint.vsac;

import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class VSACSVSHandler extends DefaultHandler {

  private ValueSet currentValueSet;
  private ValueSetExpansionComponent currentExpansion;
  private List<ValueSet> parsedValueSets;
  private boolean isInStatus;
  private StringBuilder statusStringBuilder;
  private boolean isInSource;
  private StringBuilder sourceStringBuilder;

  public VSACSVSHandler() {
    this.parsedValueSets = new ArrayList<ValueSet>();
  }

  @Override
  public void endDocument() throws SAXException {
    // TODO Auto-generated method stub
    super.endDocument();
  }

  @Override
  public void startDocument() throws SAXException {
    // TODO Auto-generated method stub
    super.startDocument();
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    switch (qName) {
      // Handle start of new valueset
      case "ns0:DescribedValueSet":
        // create value set
        this.currentValueSet = new ValueSet();
        this.currentValueSet.setId(attributes.getValue("ID"));
        this.currentValueSet.setName(attributes.getValue("displayName"));
        this.currentExpansion = new ValueSetExpansionComponent();
        this.currentExpansion.setTimestamp(new Date());
        break;

      // Handle a code/concept addition
      case "ns0:Concept":
        ValueSetExpansionContainsComponent concept = new ValueSetExpansionContainsComponent();
        concept.setCode(attributes.getValue("code"));
        concept.setVersion(attributes.getValue("codeSystemVersion"));
        concept.setDisplay(attributes.getValue("displayName"));
        concept.setSystem(CodeSystemTranslator.convertOidToUri(attributes.getValue("codeSystem")));

        this.currentExpansion.getContains().add(concept);
        break;
      
      // Handle start of Status
      case "ns0:Status":
        this.isInStatus = true;
        this.statusStringBuilder = new StringBuilder();
        break;
      
      // Handle start of Source
      case "ns0:Source":
        this.isInSource = true;
        this.sourceStringBuilder = new StringBuilder();
        break;

      default:
        // Unhandled element. Most likely something we don't need info from.
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    switch (qName) {
      // Handle end of valueset
      case "ns0:DescribedValueSet":
        this.currentExpansion.setTotal(this.currentExpansion.getContains().size());
        this.currentValueSet.setExpansion(this.currentExpansion);
        this.parsedValueSets.add(this.currentValueSet);
        this.currentValueSet = null;
        this.currentExpansion = null;
        break;
      
      // Handle end of Status
      case "ns0:Status":
        this.isInStatus = false;
        String status = this.statusStringBuilder.toString();
        if (status.equals("Active")) {
          this.currentValueSet.setStatus(PublicationStatus.ACTIVE);
        } else if (status.equals("Draft")) {
          this.currentValueSet.setStatus(PublicationStatus.DRAFT);
        } else {
          this.currentValueSet.setStatus(PublicationStatus.UNKNOWN);
        }
        this.statusStringBuilder = null;
        break;

      // Handle end of Source
      case "ns0:Source":
        this.isInSource = false;
        this.currentValueSet.setPublisher(this.sourceStringBuilder.toString());
        this.sourceStringBuilder = null;
        break;
    }
  }

  public List<ValueSet> getParsedValueSets() {
    return this.parsedValueSets;
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    if (this.isInStatus) {
      this.statusStringBuilder.append(new String(ch, start, length));
    } else if (this.isInSource) {
      this.sourceStringBuilder.append(new String(ch, start, length));
    }
  }
}