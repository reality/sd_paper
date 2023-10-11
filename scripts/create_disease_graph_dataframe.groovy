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

def labels = [:]
new File('../synonym_expansion_validation/hpo/unexpanded_all.txt').splitEachLine('\t') {
  it[1] = it[1].tokenize('/').last().replace('_',':')
  if(!labels.containsKey(it[1])) { labels[it[1]] = it[0] }
}
def allScores = new JsonSlurper().parseText(new File('data/create_output_json/data.json').text).profiles
def allClasses = [:]
allScores.each { k, v ->
  /*v.bldp.each { hp, vv -> allClasses[hp] = true }
  v.smdp.each { hp, vv -> allClasses[hp] = true }*/
  allClasses[k] = true
}

def manager = OWLManager.createOWLOntologyManager()
def fac = manager.getOWLDataFactory()
def config = new SimpleConfiguration()
def elkFactory = new ElkReasonerFactory() // cute

def ontology = manager.loadOntologyFromOntologyDocument(new File("data/hp.owl"))
def reasoner = elkFactory.createReasoner(ontology, config)

def convertIri = { it.tokenize('/').last().replace('_',':') }
def df = [ "subclass\tsuperclass" ]
def processClass
def vertexHeat = [:]
def facetChildren
def TO_PROC = 'http://purl.obolibrary.org/obo/HP_0012531'
processClass = { iri -> 
  def scs = reasoner.getSubClasses(fac.getOWLClass(IRI.create(iri)), true).collect { it.getRepresentativeElement().getIRI().toString() }.unique(false)
  scs = scs.findAll { it.indexOf('owl#Nothing') == -1 }.findAll { allClasses.containsKey(convertIri(it)) }

  if(!vertexHeat.containsKey(convertIri(iri))) {
    def scc = reasoner.getSubClasses(fac.getOWLClass(IRI.create(iri)), false).collect { it.getRepresentativeElement().getIRI().toString() }.unique(false).findAll { it.indexOf('owl#Nothing') == -1 }.collect { convertIri(it) }
    vertexHeat[iri] = [
      bldp: 0,
      smdp: 0
    ]
    allScores.collect { k, v -> 
      v.bldp.each { hpid, vv -> if(scc.contains(hpid) || convertIri(iri) == hpid) { vertexHeat[iri].bldp++ } }
      v.smdp.each { hpid, vv -> if(scc.contains(hpid) || convertIri(iri) == hpid) { vertexHeat[iri].smdp++ }}
    }
    if(iri != TO_PROC) { 
      vertexHeat[iri].bldp = vertexHeat[iri].bldp / vertexHeat[TO_PROC].bldp 
      vertexHeat[iri].smdp = vertexHeat[iri].smdp / vertexHeat[TO_PROC].smdp
    } 
  }

  scs.each { scIri ->
    df << "${labels[convertIri(iri)]}\t${labels[convertIri(scIri)]}"
if(labels[convertIri(scIri)] == null) { println scIri }
    processClass(scIri)
  }
}

facetChildren = reasoner.getSubClasses(fac.getOWLClass(IRI.create(TO_PROC)), false).collect { it.getRepresentativeElement().getIRI().toString() }.unique(false)
processClass(TO_PROC)
println vertexHeat

new File('data/create_graph_dataframe/graph_df.tsv').text = df.join('\n')
new File('data/create_graph_dataframe/vertices.tsv').text = vertexHeat.collect { k, v ->
  if(k == TO_PROC) {v.bldp = 1;v.smdp=1}
 "${labels[convertIri(k)]}\t${v.bldp}\t${v.smdp}" 
}.join('\n')
