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

// Definition for NPMI calculation...
Double npmi(Double total, Double x, Double y, Double xy) {
  Double px = x/total
  Double py = y/total
  Double pxy = xy/total
  Double pmi = Math.log(pxy/(px*py))
  Double npmi = pmi/(-1 * Math.log(pxy))
  return npmi
}

// First we load HPO and DO reasoners so that we can use them to get the superclasses of the classes in the transaction record

def manager = OWLManager.createOWLOntologyManager()
def fac = manager.getOWLDataFactory()
def config = new SimpleConfiguration()
def elkFactory = new ElkReasonerFactory() // cute

def hp = manager.loadOntologyFromOntologyDocument(new File("data/hp.owl"))
def hpReasoner = elkFactory.createReasoner(hp, config)
println "Classified HPO"

def doid = manager.loadOntologyFromOntologyDocument(new File("data/doid.owl"))
def doReasoner = elkFactory.createReasoner(doid, config)
println "Classified DOID"

// In this map we cache the superclasses of each class, method just checks/fills that etc
def scCache = [:]
def getSuperclasses = { tid ->
  if(!scCache.containsKey(tid)) { 
    def ce = fac.getOWLClass(IRI.create('http://purl.obolibrary.org/obo/' + tid.replace(':', '_')))
    if(tid =~ /DOID/) { 
      scCache[tid] = doReasoner.getSuperClasses(ce, false).collect { it.getRepresentativeElement().getIRI().toString().split('/').last().replace('_',':') }.unique(false)
    } else {
      scCache[tid] = hpReasoner.getSuperClasses(ce, false).collect { it.getRepresentativeElement().getIRI().toString().split('/').last().replace('_',':') }.unique(false)
    }
  }

  return scCache[tid]
}

// Here we're just making a map that tells us how many phenotypes and diseases we need to generate for each transaction when we do our permutations
def transactionProfile = []

Double i = 0 
Double overallCount = 0 

def allDiseases = [:]
def allPhenotypes = [:]

// Here we build the  main, actual counts, as well as build the transaction profile for the permutations
def counts = [:]
def both = [:]
def addCount = { k ->
  if(!counts.containsKey(k)) { counts[k] = 0 }  
  counts[k]++
  overallCount++
}
def pout = new PrintWriter(new BufferedWriter(new FileWriter("data/process_transactions/propagated_transactions.tsv")))
new File('./data/raw_transactions.tsv').eachLine { l ->
  if(((++i) % 1000000) == 0) { println "Processing record $i" }
  def f = l.split('\t')

  def toProcess = []
                  //  HP, DO
  def transacRecord = [0, 0]
  def a = [:]
  if(f.size() > 1 && f[1] && f[1] != '') { 
    def explicit = f[1].tokenize(';')
    explicit.each { c ->
      a[c] = true
      getSuperclasses(c).each { s -> a[s] = true }
      allPhenotypes[c] = true
    }
    a.each { k, t -> addCount(k) }
    transacRecord[0] = explicit.size()
  } 
  def b = [:]
  if(f.size() > 2 && f[2] && f[2] != '') { 
    def explicit = f[2].tokenize(';')
    explicit.each { c ->
      b[c] = true
      // we are no longer doing DO parents
      //getSuperclasses(c).each { s -> b[s] = true }
      allDiseases[c] = true
    }
    b.each { k, t -> addCount(k) }
    transacRecord[1] = explicit.size()
  }

  // Here we add to our counts and co-ocurrence information for the diseases and phenotypes
  b.each { k, t ->
    if(!both.containsKey(k)) { both[k] = [:] }
    a.each { pk, pt ->
      if(!both[k].containsKey(pk)) { both[k][pk] = 0 }
      both[k][pk]++
    }
  }

  pout.println(a.collect { it.getKey() }.join(';') + '\t' + b.collect { it.getKey() }.join(';') )

  //println "Record: ${transacRecord}"
  transactionProfile << transacRecord
  //println " "
}

pout.flush() ; pout.close()

// Should have just built them separately but eh
allDiseases = allDiseases.keySet().toList()
allPhenotypes = allPhenotypes.keySet().toList()

// Write ocurrence counts for all diseases and phenotypes
println "Writing counts..."
def cout = new PrintWriter(new BufferedWriter(new FileWriter("data/process_transactions/counts.tsv")))
counts.each { tid, tc ->
  def out = [tid, tc].join('\t')
  cout.println(out)
}
cout.flush() ; cout.close()

// Output the transaction profile, used for later permutation testing
println "Writing transaction profile..."
def tout = new PrintWriter(new BufferedWriter(new FileWriter("data/process_transactions/transaction_profile.tsv")))
transactionProfile.each { tr ->
  tout.println(tr.join('\t'))
}
tout.flush() ; tout.close()

// calculate and output NPMI values, as well as co-ocurrence numbers
println "Calculating and writing co-ocurrence values ..."
def aout = new PrintWriter(new BufferedWriter(new FileWriter("data/process_transactions/associations.tsv")))
allDiseases.each { d ->
  def disnum = counts[d]
  allPhenotypes.each { p ->
    def idx = i
    def phenonum = counts[p]

    def bot = 0 
    if(both.containsKey(d) && both[d].containsKey(p)) {
      bot = both[d][p] 
    }

    aout.println([d, p, bot, npmi(idx, disnum, phenonum, bot)].join('\t'))
  }
}
aout.flush() ; aout.close()

println 'Done'
