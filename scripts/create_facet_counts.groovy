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

def diseaseOntology = manager.loadOntologyFromOntologyDocument(new File("data/hp.owl"))
def reasoner = elkFactory.createReasoner(diseaseOntology, config)

def facets = new File('data/facets.txt').text.split('\n').collect { it.split('\t') }
def allAbnormalities = reasoner.getSubClasses(fac.getOWLClass(IRI.create('http://purl.obolibrary.org/obo/HP_0000118')), false).collect { it.getRepresentativeElement().getIRI().toString() }.unique(false).size()
def facetProps = facets.collectEntries { 
  def ce = fac.getOWLClass(IRI.create(it[1]))
  [(it[0]): reasoner.getSubClasses(ce, false).collect { it.getRepresentativeElement().getIRI().toString() }.unique(false).size() / allAbnormalities]
}

def bPWriter = new BufferedWriter(new FileWriter('data/create_facet_counts/bldp_facet_profiles.tsv'))
def sPWriter = new BufferedWriter(new FileWriter('data/create_facet_counts/smdp_facet_profiles.tsv'))
def sPNWriter = new BufferedWriter(new FileWriter('data/create_facet_counts/smdp_novel_facet_profiles.tsv'))
def ukbWriter = new BufferedWriter(new FileWriter('data/create_facet_counts/ukb_novel_facet_profiles.tsv'))
def c1 = 0
def c2 = 0
def c3 = 0
def c4 = 0
def allScores = new JsonSlurper().parseText(new File('data/create_output_json/data.json').text).profiles

def novelFacetCounts = [:]
def facetCounts = [:]
facets.each { facet ->
  facetCounts[facet[0]] = [
    ws: 0,
    dp: 0,
    wsn: 0,
    ukb: 0
  ]

  def ce = fac.getOWLClass(IRI.create(facet[1]))
  def scs = reasoner.getSubClasses(ce, false).collect { it.getRepresentativeElement().getIRI().toString().split('/').last()replace('_',':') }.unique(false)
  
  allScores.each { doid, ass ->
  println ass.bldp.size()
    if(!(ass.bldp.size() > 0)) { return; }

    ass.smdp.each { k, v -> 
      if(!v.significant) { return; }
      if(scs.contains(k)) { 
        facetCounts[facet[0]].ws++ 
        if(v.novel) {
          facetCounts[facet[0]].wsn++
          sPNWriter.write("${c3++}\t$k\tsmdp_novel_${facet[0].trim()};all\n")
        }
        sPWriter.write("${c1++}\t$k\tsmdp_all_${facet[0].trim()};all\n")
      } 
    } 
    ass.bldp.each { k, v -> 
    if(scs.contains(k)) { 
      facetCounts[facet[0]].dp++ 
      bPWriter.write("${c2++}\t$k\tbldp_all_${facet[0].trim()};all\n")
    } 
    } 

    ass.ukbb.each { k, v ->
    if(scs.contains(k)) { 
      facetCounts[facet[0]].ukb++ 
      ukbWriter.write("${c4++}\t$k\tukb_all_${facet[0].trim()};all\n")
    } 
    }
  }
}

bPWriter.flush() ; bPWriter.close()
sPWriter.flush() ; sPWriter.close()
sPNWriter.flush() ; sPNWriter.close()
ukbWriter.flush() ; ukbWriter.close()

def dTotal = facetCounts.collect { k, v -> v.dp }.sum()
facetCounts.each { k, v -> v.dp = (v.dp / dTotal) / facetProps[k] }

def wTotal = facetCounts.collect { k, v -> v.ws }.sum()
facetCounts.each { k, v -> v.ws = (v.ws / wTotal)  / facetProps[k] }

def wnTotal = facetCounts.collect { k, v -> v.wsn }.sum()
facetCounts.each { k, v -> v.wsn = (v.wsn / wnTotal)  / facetProps[k] }

def ukTotal = facetCounts.collect { k, v -> v.ukb }.sum()
facetCounts.each { k, v -> v.ukb = (v.ukb / ukTotal)  / facetProps[k] }


def out = 'facet\tSocial Media\tLiterature\tSocial Media Novel\n' + facetCounts.collect { k, v -> "$k\t${v.ws}\t${v.dp}\t${v.wsn}" }.join('\n').replaceAll('0E\\+10','0')
new File('data/create_facet_counts/facet_counts.tsv').text = out
