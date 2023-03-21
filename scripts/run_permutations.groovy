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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.*
import groovyx.gpars.GParsPool

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
def getSuperclasses = { o, tid ->
  if(!scCache.containsKey(tid)) { 
    def ce = fac.getOWLClass(IRI.create('http://purl.obolibrary.org/obo/' + tid.replace(':', '_')))
    if(o == 'DO') { 
      scCache[tid] = doReasoner.getSuperClasses(ce, false).collect { it.getRepresentativeElement().getIRI().toString().split('/').last().replace('_',':') }.unique(false)
    } else {
      scCache[tid] = hpReasoner.getSuperClasses(ce, false).collect { it.getRepresentativeElement().getIRI().toString().split('/').last().replace('_',':') }.unique(false)
    }
  }

  return scCache[tid]
}

println "Loading data from process_transactions..."
def fullTransac = new File('data/process_transactions/transaction_profile.tsv').text.split('\n').collect { it.tokenize('\t').collect { i -> Integer.parseInt(i) } }
def nr = new Random(42069)

def allDiseases = [:]
def allPhenotypes = [:]
println "Loaded transaction profile..."

new File('data/process_transactions/counts.tsv').splitEachLine('\t') { if(it[0] =~ /DOID/) { allDiseases[it[0]] = true } else { allPhenotypes[it[0]] = true } }
allDiseases = allDiseases.keySet().toList()
allPhenotypes = allPhenotypes.keySet().toList()
println "Loaded diseases/phenotypes"

// might as well only calc for ones with positive values, we don't really care otherwise
def weCare = [:]
def careassocs = 0
new File('data/process_transactions/associations.tsv').splitEachLine('\t') {
  if(!weCare.containsKey(it[0])) { weCare[it[0]] = [:] }
  if(it[3] == 'NaN') { return; }
  if(Double.parseDouble(it[3]) > 0) { weCare[it[0]][it[1]] = true; careassocs++ }
}
println "found the combos we care about: $careassocs"

println "diseases ${allDiseases.size()}"
println "phenotypes ${allPhenotypes.size()}"
println "total transactions ${fullTransac.size()}"

println "Building permutations..."
def PERMS = 2000
//def PERMUTATION_SUBSET = 50
def results = []

(0..<PERMS).each { permCount ->
  println "Calculating permutation $permCount"

  def counts = new ConcurrentHashMap()
  def both = new ConcurrentHashMap()
  allDiseases.each { counts[it] = 0 }
  allPhenotypes.each { counts[it] = 0 }
  allDiseases.each { k ->
    both[k] = new ConcurrentHashMap()
    allPhenotypes.each { kk ->
      if(weCare.containsKey(k) && weCare[k].containsKey(kk)) {
        both[k][kk] = new AtomicInteger()
      }
    }
  }

  def i = new AtomicInteger(0)
  //def transactionProfile = (0..<fullTransac.size()).collect { fullTransac[nr.nextInt(fullTransac.size())] }
  def transactionProfile = fullTransac
  println "sampled transactions ${transactionProfile.size()}"

  GParsPool.withPool {
  transactionProfile.eachParallel { tp ->
    if((i.getAndIncrement() % 1000000) == 0 ) { println i }
    def r = ThreadLocalRandom.current()
    // Generate random phenotypes
    def phenotypes = [:]
    def explicitPhenotypes = [:]
    // transaction profile is actually [0] = disease [1] = phenotypes (opposite to the actual transaction file for some stupid reason)
    (0..<tp[1]).each { explicitPhenotypes[allPhenotypes[r.nextInt(allPhenotypes.size())]] = true }
    explicitPhenotypes.each { k, t ->
      phenotypes[k] = true
      getSuperclasses('HP', k).each { s -> phenotypes[s] = true } 
    }

    def diseases = [:]
    def explicitDiseases = [:]
    (0..<tp[0]).each { explicitDiseases[allDiseases[r.nextInt(allDiseases.size())]] = true }
    explicitDiseases.each { k, t ->
      diseases[k] = true
      getSuperclasses('DO', k).each { s -> diseases[s] = true } 
    }

    def toProcess = diseases + phenotypes
    toProcess.each { k, t -> counts[k]++ }

    diseases.keySet().toList().each { k ->
      if(k == 'owl#Thing') { return; }
      phenotypes.each { pk, pt ->
        //if(!both[k].containsKey(pk)) { both[k][pk] = new AtomicInteger() }
        if(both.containsKey(k) && both[k].containsKey(pk)) {
          both[k][pk].getAndIncrement()
        }
      }
    }
  }
  }

  def res = [ 'counts': counts, associations: [:] ]

  results << [ 'counts': counts, 'both': both ]

  if(results.size() == 10) {
    def fname = (permCount-10) + '-' + permCount

    println "Writing counts..."
    def cout = new PrintWriter(new BufferedWriter(new FileWriter("data/run_permutations/${fname}_counts.tsv")))
    results[0].counts.each { tid, tc ->
      def out = [tid, results.collect { it.counts[tid] }.join(',') ].join('\t')
      cout.println(out)
    }
    cout.flush() ; cout.close()

    println "Calculating and writing co-ocurrence values for $permCount..."
    def aout = new PrintWriter(new BufferedWriter(new FileWriter("data/run_permutations/${fname}_associations.tsv")))
    def idx = i.doubleValue()
    allDiseases.each { d ->
      allPhenotypes.each { p ->
        if(!weCare.containsKey(d)) { return } 
        if(!weCare[d].containsKey(p)) { return; }

        def npmis = results.collect { r ->
          def disnum = r.counts[d]
          def phenonum = r.counts[p]

          def bot = 0 
          if(r.both.containsKey(d) && r.both[d].containsKey(p)) {
            bot = r.both[d][p].intValue()
          }

          [bot, npmi(idx, disnum, phenonum, bot).round(4)]
        }
        
        aout.println([d, p, npmis.collect { it[1] } .join(',')].join('\t'))
      }
    }
    aout.flush() ; aout.close()

    results = []
  }
}
