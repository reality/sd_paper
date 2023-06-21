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
def reasoner = elkFactory.createReasoner(diseaseOntology, config)


def hpOntology = manager.loadOntologyFromOntologyDocument(new File("data/hp.owl"))
def hpReasoner = elkFactory.createReasoner(hpOntology, config)

def facets = new File('data/disease_facets.tsv').text.split('\n').collect { it.split('\t') }

def allScores = new JsonSlurper().parseText(new File('data/create_output_json/data.json').text).profiles

def dFCounts = [:]
def facetCounts = [:]
def conScs = hpReasoner.getSubClasses(fac.getOWLClass(IRI.create('http://purl.obolibrary.org/obo/HP_0025142')), false).collect { it.getRepresentativeElement().getIRI().toString().split('/').last()replace('_',':') }.unique(false)
println conScs
facets.each { facet ->
  facetCounts[facet[0]] = [
    wsn: 0,
    cs: 0,
    blcs: 0
  ]
  dFCounts[facet[0]] = 0

  def ce = fac.getOWLClass(IRI.create(facet[1]))
  def scs = reasoner.getSubClasses(ce, false).collect { it.getRepresentativeElement().getIRI().toString().split('/').last()replace('_',':') }.unique(false)
  
  allScores.each { doid, ass ->
    if(scs.contains(doid)) {
      facetCounts[facet[0]].wsn += ass.smdp.findAll { k, v -> v.novel }.size()

      def novelCons = ass.smdp.findAll { k, v -> conScs.contains(k) }.size()
      facetCounts[facet[0]].cs += novelCons

      facetCounts[facet[0]].blcs += ass.bldp.findAll { k, v -> conScs.contains(k) }.size()

      dFCounts[facet[0]]++
    }
  }
}



// total of novel phenotypes
def wnTotal = facetCounts.collect { k, v -> v.wsn }.sum()
def totalCon = facetCounts.collect { k, v -> v.cs }.sum()
def totalBlCon = facetCounts.collect { k, v -> v.blcs }.sum()
println totalCon
facetCounts.each { k, v -> 
  v.wsn = (v.wsn / wnTotal)
  v.dc = (dFCounts[k] / dFCounts.collect { it.getValue() }.sum()) 
  v.blcs = (v.blcs / totalBlCon) / v.dc
  v.cs = (v.cs / totalCon) / v.dc
}

new File('data/create_disease_facet_counts/facet_counts.tsv').text = 'facet\tDiseases\tSocial Media Novel\tBLDP Constitutional Symptoms\tConstitutional Symptoms\n' + facetCounts.collect { k, v -> "$k\t${v.dc}\t${v.wsn}\t${v.blcs}\t${v.cs}" }.join('\n')
