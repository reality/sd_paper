@Grapes([
    @Grab(group='net.sourceforge.owlapi', module='owlapi-api', version='5.1.14'),
    @Grab(group='net.sourceforge.owlapi', module='owlapi-apibinding', version='5.1.14'),
    @Grab(group='net.sourceforge.owlapi', module='owlapi-impl', version='5.1.14'),
    @Grab(group='net.sourceforge.owlapi', module='owlapi-parsers', version='5.1.14'),
    @Grab(group='net.sourceforge.owlapi', module='owlapi-distribution', version='5.1.14'),

  @GrabResolver(name='sonatype-nexus-snapshots', root='https://oss.sonatype.org/service/local/repositories/snapshots/content/'),
   // @Grab('org.semanticweb.elk:elk-reasoner:0.5.0-SNAPSHOT'),
   // @Grab('org.semanticweb.elk:elk-owl-implementation:0.5.0-SNAPSHOT'),
    @Grab('au.csiro:elk-owlapi5:0.5.0'),
  
    @GrabConfig(systemClassLoader=true)
])

import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.parameters.*
import org.semanticweb.elk.owlapi.*
import org.semanticweb.elk.reasoner.config.*
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.reasoner.*
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.io.*
import org.semanticweb.owlapi.owllink.*
import org.semanticweb.owlapi.util.*
import org.semanticweb.owlapi.search.*
import org.semanticweb.owlapi.manchestersyntax.renderer.*
import org.semanticweb.owlapi.reasoner.structural.*
import org.semanticweb.elk.reasoner.config.*
import org.semanticweb.owlapi.apibinding.*
import org.semanticweb.owlapi.reasoner.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*
import groovyx.gpars.*
import org.codehaus.gpars.*
import groovy.json.*

def manager = OWLManager.createOWLOntologyManager()
def fac = manager.getOWLDataFactory()
def config = new SimpleConfiguration()
def elkFactory = new ElkReasonerFactory() // cute

def diseaseOntology = manager.loadOntologyFromOntologyDocument(new File("data/doid.owl"))
def doReasoner = elkFactory.createReasoner(diseaseOntology, config)
def hpOntology = manager.loadOntologyFromOntologyDocument(new File('data/hp.owl'))
def hpReasoner = elkFactory.createReasoner(hpOntology, config)

def disease_facets = [ 
['neoplasm',	'http://purl.obolibrary.org/obo/DOID_14566'],
['nervous system',	'http://purl.obolibrary.org/obo/DOID_863'],
['endocrine',	'http://purl.obolibrary.org/obo/DOID_28'],
['metabolic',	'http://purl.obolibrary.org/obo/DOID_0014667'],
['cardiovascular',	'http://purl.obolibrary.org/obo/DOID_1287'],
['immune',	'http://purl.obolibrary.org/obo/DOID_2914'],
['digestive',	'http://purl.obolibrary.org/obo/DOID_77'],
['musculoskeletal',	'http://purl.obolibrary.org/obo/DOID_17'],
['urinary tract',	'http://purl.obolibrary.org/obo/DOID_18'],
['reproductive',	'http://purl.obolibrary.org/obo/DOID_15'],
['blood disorder',	'http://purl.obolibrary.org/obo/DOID_74'],
['integumentary',	'http://purl.obolibrary.org/obo/DOID_16'],
['respiratory',	'http://purl.obolibrary.org/obo/DOID_1579'],
['thoracic',	'http://purl.obolibrary.org/obo/DOID_0060118'],

// maybe we can leave a break between these in the graph because they don't have friends (sorry)
['infectious', 'http://purl.obolibrary.org/obo/DOID_0050117'],
['mental health',	'http://purl.obolibrary.org/obo/DOID_150'],
['genetic disease',	'http://purl.obolibrary.org/obo/DOID_630'],
['physical disorder',	'http://purl.obolibrary.org/obo/DOID_0080015'],
]

def phenotype_facets = [
['neoplasm',	'http://purl.obolibrary.org/obo/HP_0002664'],	
['nervous system', 	'http://purl.obolibrary.org/obo/HP_0000707'],	
['endocrine', 	'http://purl.obolibrary.org/obo/HP_0000818'],	
['metabolism/homeostasis',	'http://purl.obolibrary.org/obo/HP_0001939'],	
['cardiovascular', 	'http://purl.obolibrary.org/obo/HP_0001626'],	
['immune', 	'http://purl.obolibrary.org/obo/HP_0002715'],	
['digestive', 	'http://purl.obolibrary.org/obo/HP_0025031'],	
['musculoskeletal', 	'http://purl.obolibrary.org/obo/HP_0033127'],	
['genitourinary', 	'http://purl.obolibrary.org/obo/HP_0000119'],	
['prenatal or birth',	'http://purl.obolibrary.org/obo/HP_0001197'],	
['blood',	'http://purl.obolibrary.org/obo/HP_0001871'],	
['integument',	'http://purl.obolibrary.org/obo/HP_0001574'],	
['respiratory', 	'http://purl.obolibrary.org/obo/HP_0002086'],	
['thoracic cavity',	'http://purl.obolibrary.org/obo/HP_0045027'],	

// non matchers
['voice',	'http://purl.obolibrary.org/obo/HP_0001608'],	
['growth abnormality',	'http://purl.obolibrary.org/obo/HP_0001507'],	
['cellular',	'http://purl.obolibrary.org/obo/HP_0025354'],	
['limbs',	'http://purl.obolibrary.org/obo/HP_0040064'],	
['constitutional',	'http://purl.obolibrary.org/obo/HP_0025142'],	
['head or neck',	'http://purl.obolibrary.org/obo/HP_0000152'],	
]

// So we want a matrix where for each disease category, we identify the number of novel phenotypes appearing in each phenotype category
def allScores = new JsonSlurper().parseText(new File('data/create_output_json/data.json').text).profiles
def facetMatrix = [:]
def phenotypeFacets = phenotype_facets.collectEntries { 
  def (name, iri) = it
  [ (name): hpReasoner.getSubClasses(fac.getOWLClass(IRI.create(iri)), false).collect { it.getRepresentativeElement().getIRI().toString().split('/').last()replace('_',':') }.unique(false) ]
}

disease_facets.each { 
  def (name, iri) = it
  def allDiseasesInFacet = doReasoner.getSubClasses(fac.getOWLClass(IRI.create(iri)), false).collect { it.getRepresentativeElement().getIRI().toString().split('/').last()replace('_',':') }.unique(false)

  facetMatrix[name] = phenotypeFacets.collectEntries { [ (it.getKey()): 0 ] }
  allScores.findAll { allDiseasesInFacet.contains(it.getKey()) }.each { doid, associations ->
  if(!(associations.bldp.size() > 0)) { return; }
    associations.smdp.each { hpId, v ->
      phenotypeFacets.each { pFacet, members ->
        if(v.novel && v.significant && members.contains(hpId)) { facetMatrix[name][pFacet]++ }
      }
    }
  }
}

def out = [ phenotype_facets.collect { it[0] }.join(',') ]
disease_facets.each {
  def newLine = [ it[0] ]
  phenotype_facets.each { p -> newLine << facetMatrix[it[0]][p[0]] }
  out << newLine.join(',')
}

new File('data/facet_by_disease/matrix.csv').text = out.join('\n')
