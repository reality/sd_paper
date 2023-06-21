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
def hpOntology = manager.loadOntologyFromOntologyDocument(new File("data/hp.owl"))
def reasoner = elkFactory.createReasoner(hpOntology, config)

def ics = [:] ; new File('./data/get_ic/phens_ic.tsv').splitEachLine('\t') { ics[it[0].tokenize('/').last().replace('_',':')] = Float.parseFloat(it[1]) }

def allScores = new JsonSlurper().parseText(new File('data/create_output_json/data.json').text).profiles

def OBO_PREF = 'http://purl.obolibrary.org/obo/'
def depthMap = [:]
def processChild
processChild = { cid, depth ->
  depthMap[cid] = depth

  depth += 1
  reasoner.getSubClasses(fac.getOWLClass(IRI.create( OBO_PREF + cid.replace(':', '_'))), true).collect { it.getRepresentativeElement().getIRI().toString().split('/').last()replace('_',':') }.unique(false).each {
    if(!depthMap.containsKey(it)) {
      processChild(it, depth)
    }
  }
}
processChild('HP:0012531', 1) 

println depthMap 
 
def bldp = []
def smdp = []
allScores.each { doid, ass ->
  ass.smdp.each { k, v -> 
    if(depthMap.containsKey(k) && v.novel) { smdp << depthMap[k] } 
  } 
  ass.bldp.each { k, v -> 
    if(depthMap.containsKey(k)) { bldp << depthMap[k] } 
  } 
}

println bldp
println smdp

def bldpavg = (bldp.sum() / bldp.size())
def smdpavg = (smdp.sum() / smdp.size())
println "bldp: " + bldpavg
println "smdp: " + smdpavg

