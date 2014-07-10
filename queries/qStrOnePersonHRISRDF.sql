#
# qStrOnePersonHRISRDF
# with an emplID, 
# construct all HRIS RDF about a person
# updated 140425
# added hr preferred and legal name parts
# attach to VIVO vcard

PREFIX hr: <http://vivo.cornell.edu/ns/hr/0.9/hr.owl#>  
PREFIX cuvivo: <http://vivo.cornell.edu/individual/> 
PREFIX foaf: <http://xmlns.com/foaf/0.1/> 
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> 
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> 
PREFIX vivo: <http://vivoweb.org/ontology/core#> 
PREFIX newhr: <http://vivoweb.org/ontology/newhr#>
PREFIX vivo: <http://vivo.library.cornell.edu/ns/0.1#>
PREFIX titlemap: <http://vivo.library.cornell.edu/ns/hr/titleMapping#> 
PREFIX vcard: <http://www.w3.org/2006/vcard/ns#>
PREFIX obo: <http://purl.obolibrary.org/obo/>

CONSTRUCT { 
    #?hrisperson rdf:type foaf:Person .
    ?hrisperson hr:emplId "VARVALUE" .
    ?hrisperson rdfs:label ?hrislabel .
    ?hrisperson hr:netId ?hrisnetId .
    ?hrisperson hr:Unitid ?hrisUnitId .
    ?hrisperson hr:emplId ?hrisemplId .
  
    #?hrisperson hr:WorkingTitle ?hrwtitle .
    #?hrisperson vivo:email ?hremail .
  
   ?hrisperson obo:ARG_2000028 ?vivovcard . 
   ?vivovcard obo:ARG_2000029 ?hrisperson . 
   ?vivovcard vcard:hasName ?nameUri .
   ?nameUri a vcard:Name . 
   ?nameUri vcard:familyName ?hrislastName . 
    ?nameUri vcard:givenName ?hrisfirstName . 
  ?vivovcard vcard:hasEmail ?emailUri .
    ?emailUri rdf:type vcard:Email .
    ?emailUri vcard:email ?hrisemail .
  ?vivovcard vcard:hasTitle ?titleUri . 
    ?titleUri rdf:type vcard:Title .
    ?titleUri vcard:title ?hriswtitle . 
    ?hrisperson hr:primaryWorkingTitle ?primaryTitle .
    ?hrisperson hr:legalNameString ?hrisLegalNameString . 
    ?hrisperson hr:preferredFirstName ?hrisPreferredFirstName . 
    ?hrisperson hr:preferredLastName ?hrisPreferredLastName . 
    ?hrisperson hr:preferredMiddleInitial ?hrisPreferredMiddleInitial . 
    ?hrisperson hr:preferredMiddleName ?hrisPreferredMiddleName . 
    ?hrisperson hr:preferredNameString ?hrisPreferredNameString . 
    ?hrisperson hr:preferredSocialSuffix ?hrisPreferredSocialSuffix. 
    ?hrisperson hr:primaryUnitCode ?hrisPrimaryUnitCode . 
    ?hrisperson vivo:middleInitial ?hrisMiddleInitial . 
    ?hrisperson hr:WorkingTitle ?hriswtitle .
    ?hrisperson vivo:email ?hrisemail .
    ?hrisperson foaf:firstName ?hrisfirstName .
    ?hrisperson foaf:lastName ?hrislastName .
    ?hrisperson hr:PrefName ?hrisPrefName .
  }   
WHERE {
  BIND (str("VARVALUE") as ?variableEmplId)
  # querying HRIS endpoint 
  SERVICE <HRISSERV>
    {

    #?hrisperson rdf:type foaf:Person .
    ?hrisperson hr:emplId ?variableEmplId .
    ?hrisperson rdfs:label ?hrislabel .
    ?hrisperson hr:netId ?hrisnetId .
    ?hrisperson hr:Unitid ?hrisUnitId .
    ?hrisperson hr:emplId ?hrisemplId .
    OPTIONAL { ?hrisperson hr:legalNameString ?hrisLegalNameString . }
    OPTIONAL { ?hrisperson hr:preferredFirstName ?hrisPreferredFirstName . }
    OPTIONAL { ?hrisperson hr:preferredLastName ?hrisPreferredLastName . }
    OPTIONAL { ?hrisperson hr:preferredMiddleInitial ?hrisPreferredMiddleInitial . }
    OPTIONAL { ?hrisperson hr:preferredMiddleName ?hrisPreferredMiddleName . }
    OPTIONAL { ?hrisperson hr:preferredNameString ?hrisPreferredNameString . }
    OPTIONAL { ?hrisperson hr:preferredSocialSuffix ?hrisPreferredSocialSuffix. }
    OPTIONAL { ?hrisperson hr:primaryUnitCode ?hrisPrimaryUnitCode . }
    OPTIONAL { ?hrisperson vivo:middleInitial ?hrisMiddleInitial . }
    OPTIONAL { ?hrisperson hr:WorkingTitle ?hriswtitle .}
    OPTIONAL { ?hrisperson vivo:email ?hrisemail .}
    OPTIONAL { ?hrisperson foaf:firstName ?hrisfirstName .}
    OPTIONAL { ?hrisperson foaf:lastName ?hrislastName .}
    OPTIONAL { ?hrisperson hr:PrefName ?hrisPrefName .}
   }
   
   SERVICE <VIVOSERV>
    {
     ?vivoperson rdf:type foaf:Person .
     ?vivoperson hr:emplId ?variableEmplId .
     OPTIONAL { ?vivoperson obo:ARG_2000028 ?vivovcard . }
   
     OPTIONAL { ?vivovcard vcard:hasName ?nameUri .
        OPTIONAL { ?nameUri vcard:givenName ?givenName . }
        OPTIONAL { ?nameUri vcard:familyName ?familyName . }
     }
     OPTIONAL { ?vivovcard vcard:hasEmail ?emailUri .
         OPTIONAL { ?emailUri vcard:email ?emailString .}
     }
     OPTIONAL { ?vivovcard vcard:hasTitle ?titleUri . 
         OPTIONAL { ?titleUri vcard:title ?titleString . }
     }   
    
    } 
}