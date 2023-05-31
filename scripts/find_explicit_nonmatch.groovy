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

def ic = [:]
new File('data/get_ic/disease_ic.tsv').splitEachLine('\t') {
  ic[it[0].tokenize('/').last().replace('_',':')] = Float.parseFloat(it[1])
}
new File('data/get_ic/phens_ic.tsv').splitEachLine('\t') {
  ic[it[0].tokenize('/').last().replace('_',':')] = Float.parseFloat(it[1])
}

def labels = [:]
new File('../synonym_expansion_validation/do/disease_unexpanded.txt').splitEachLine('\t') {
  it[1] = it[1].tokenize('/').last().replace('_',':')
  if(!labels.containsKey(it[1])) { labels[it[1]] = it[0] }
}
new File('../synonym_expansion_validation/hpo/unexpanded_all.txt').splitEachLine('\t') {
  it[1] = it[1].tokenize('/').last().replace('_',':')
  if(!labels.containsKey(it[1])) { labels[it[1]] = it[0] }
}

def IC_CUTOFF = 0
def PARENTFIX = false

def litAssoc = [:]
def disCutoffs = [:]
new File('./data/match_litphens/final_mappings.tsv').splitEachLine('\t') {
  if(PARENTFIX && ic[it[1]] < IC_CUTOFF) { return; }
  if(!litAssoc.containsKey(it[0])) {
    litAssoc[it[0]] = []
    disCutoffs[it[0]] = 0
  }
  litAssoc[it[0]] << it[1]
}
litAssoc.collectEntries { k, v -> [(k): v.unique(false)] }

// Load social media associations
def smAssoc = [:]
new File('./data/qval/associations.tsv').splitEachLine('\t') {
  if(PARENTFIX && ic[it[1]] < IC_CUTOFF) { return; }
  if(it[0] == 'X1') { return; }
  if(litAssoc.containsKey(it[0])) { matched = true }
  def pmi = Double.parseDouble(it[2])
  if(pmi > 0) { 
    if(!smAssoc.containsKey(it[0])) { smAssoc[it[0]] = [] }
    smAssoc[it[0]] << [ it[1], pmi, it[5] ]
  } 
}

// remove colinear bois
smAssoc = smAssoc.collectEntries { k, v -> [(k): v.findAll { co -> co[1] > 0.75 ? false : true }] }

def matchedDiseases = smAssoc.findAll { k, v -> litAssoc.containsKey(k) }
def matchCount = matchedDiseases.collect{ k, v -> v.size() }.sum()

def mout = []
def litcount = 0
smAssoc.each { k, v ->
  mout << [ k, v.collect { it.join(',') }.join(';'), litAssoc.containsKey(k) ? litAssoc[k].join(';') : 'false' ].join('\t')
  if(litAssoc.containsKey(k)) {
    litcount += litAssoc[k].size()
  }
}
new File('data/find_explicit_nonmatch/matched_profiles.tsv').text = mout.join('\n')

println "Matched diseases: ${matchedDiseases.size()}  (of ${smAssoc.size()}) with $matchCount social phens and $litcount literature phens"

println 'signif' + matchedDiseases.collect { it.getValue().findAll{ v -> v[2] == 'TRUE' }.size() }.sum()

def manager = OWLManager.createOWLOntologyManager()
def fac = manager.getOWLDataFactory()
def config = new SimpleConfiguration()
def elkFactory = new ElkReasonerFactory() // cute

def hp = manager.loadOntologyFromOntologyDocument(new File("data/hp.owl"))
def reasoner = elkFactory.createReasoner(hp, config)

def smOnly = [:]
def i = 0
smAssoc.each { k, v ->
  println "${++i}/${smAssoc.size()}"
	smOnly[k] = v.collectEntries { co ->
    def tid = co[0]
    def ce = fac.getOWLClass(IRI.create('http://purl.obolibrary.org/obo/' + tid.replace(':', '_')))
    def subclasses = reasoner.getSubClasses(ce, false).collect { it.getRepresentativeElement().getIRI().toString().split('/').last().replace('_',':') }.unique(false)
    def superclasses = reasoner.getSuperClasses(ce, false).collect { it.getRepresentativeElement().getIRI().toString().split('/').last().replace('_',':') }.unique(false)

    [(co[0]): [
      novel: litAssoc.containsKey(k) ? !litAssoc[k].contains(tid) && !subclasses.any { litAssoc[k].contains(it) } : true,
      laconic: !subclasses.any { smAssoc[k].any { b -> b[0] == it && b[2] == 'TRUE' } }, // laconic if there are no subclasses that are significant
      npmi: co[1],
      significant: co[2]
    ]]
	}

  println "Found ${smOnly[k].size()} phenotypes out of ${smAssoc[k].size()} (lit phenotypes: ${litAssoc.containsKey(k) ? litAssoc[k].size() : 'none'})!"
}

def out = []
smOnly.each { k, v ->
  k = k+'\t'+labels[k] 
  v.each { ii, jj ->
    def oo = "$ii\t${labels[ii]}\tlaconic:${jj.laconic}\tnovel:${jj.novel}\tnpmi:${jj.npmi}\tsignificant:${jj.significant}"
    out << "$k\t"+oo
  } 
}

new File('data/find_explicit_nonmatch/explicit_smonly.tsv').text = out.join('\n')
