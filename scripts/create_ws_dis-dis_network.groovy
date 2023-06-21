@Grab(group='com.github.sharispe', module='slib-sml', version='0.9.1')

import groovy.json.*
import groovyx.gpars.*
import org.codehaus.gpars.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*
 
import slib.graph.algo.utils.GAction;
import slib.graph.algo.utils.GActionType;

import org.openrdf.model.URI;
import slib.graph.algo.accessor.GraphAccessor;
//import slib.graph.algo.extraction.utils.GAction;
//import slib.graph.algo.extraction.utils.GActionType;
import slib.graph.algo.validator.dag.ValidatorDAG;
import slib.graph.io.conf.GDataConf;
import slib.graph.io.conf.GraphConf;
import slib.graph.io.loader.GraphLoaderGeneric;
import slib.graph.io.util.GFormat;
import slib.graph.model.graph.G;
import slib.graph.model.impl.graph.memory.GraphMemory;
import slib.graph.model.impl.repo.URIFactoryMemory;
import slib.graph.model.repo.URIFactory;
import slib.sml.sm.core.engine.SM_Engine;
import slib.sml.sm.core.metrics.ic.utils.IC_Conf_Topo;
import slib.sml.sm.core.metrics.ic.utils.ICconf;
import slib.graph.algo.extraction.utils.*
import slib.sglib.io.loader.*
import slib.sml.sm.core.metrics.ic.utils.*
import slib.sml.sm.core.utils.*
import slib.sglib.io.loader.bio.obo.*
import org.openrdf.model.URI
import slib.graph.algo.extraction.rvf.instances.*
import slib.sglib.algo.graph.utils.*
import slib.utils.impl.Timer
import slib.graph.algo.extraction.utils.*
import slib.graph.model.graph.*
import slib.graph.model.repo.*
import slib.graph.model.impl.graph.memory.*
import slib.sml.sm.core.engine.*
import slib.graph.io.conf.*
import slib.graph.model.impl.graph.elements.*
import slib.graph.algo.extraction.rvf.instances.impl.*
import slib.graph.model.impl.repo.*
import slib.graph.io.util.*
import slib.graph.io.loader.*

import slib.sml.sm.core.metrics.ic.utils.IC_Conf_Corpus;
import slib.sml.sm.core.utils.SMConstants;
import slib.sml.sm.core.utils.SMconf;
import slib.utils.ex.SLIB_Exception;
import slib.utils.impl.Timer;

def allScores = new JsonSlurper().parseText(new File('data/create_output_json/data.json').text).profiles.collectEntries { k, v ->
  [(k): [ 
    ws: v.smdp.collect { it.getKey() },
    dp: v.bldp.collect { it.getKey() }
  ]]
}.findAll { k, v -> v.ws != null && v.dp != null }

println allScores

println 'writing the annotation file now'
def ANNOT_PATH = 'data/create_ws_dis-dis_network/annot.tsv'
def sWriter = new BufferedWriter(new FileWriter(ANNOT_PATH))
allScores.each {d1, v1 ->
  def all = v1.ws + v1.dp
  sWriter.write("$d1\t${all.join(';')}\n")
}
sWriter.close()
def oo = ''
def y = 0

URIFactory factory = URIFactoryMemory.getSingleton()

def ontoFile = 'data/hp.owl'
def graphURI = factory.getURI('http://HP/')
factory.loadNamespacePrefix("HP", graphURI.toString());

G graph = new GraphMemory(graphURI)

def dataConf = new GDataConf(GFormat.RDF_XML, ontoFile)
def actionRerootConf = new GAction(GActionType.REROOTING)
actionRerootConf.addParameter("root_uri", "HP:0000001"); // phenotypic abnormality
//actionRerootConf.addParameter("root_uri", "DOID:4"); // phenotypic abnormality

def gConf = new GraphConf()
gConf.addGDataConf(dataConf)
gConf.addGAction(actionRerootConf)
//def gConf = new GraphConf()
//gConf.addGDataConf(dataConf)
gConf.addGDataConf(new GDataConf(GFormat.TSV_ANNOT, ANNOT_PATH));

GraphLoaderGeneric.load(gConf, graph)

def roots = new ValidatorDAG().getTaxonomicRoots(graph)

def icConf = new IC_Conf_Corpus(SMConstants.FLAG_IC_ANNOT_RESNIK_1995)
//def icConf = new IC_Conf_Topo(SMConstants.FLAG_ICI_ZHOU_2008)
//def icConf = new IC_Conf_Topo(SMConstants.FLAG_ICI_SANCHEZ_2011)
def smConfPairwise = new SMconf(SMConstants.FLAG_SIM_PAIRWISE_DAG_NODE_RESNIK_1995, icConf)
//def smConfPairwise = new SMconf(SMConstants.FLAG_SIM_PAIRWISE_DAG_NODE_LIN_1998, icConf)

def smConfGroupwise = new SMconf(SMConstants.FLAG_SIM_GROUPWISE_AVERAGE, icConf)
// uncomment to do the bma
//def smConfGroupwise = new SMconf(SMConstants.FLAG_SIM_GROUPWISE_BMA, icConf)

//def smConfGroupwise = new SMconf(SMConstants.FLAG_SIM_GROUPWISE_AVERAGE_NORMALIZED_GOSIM, icConf)
// FLAG_SIM_GROUPWISE_AVERAGE_NORMALIZED_GOSIM

//def smConfPairwise = new SMconf(SMConstants.FLAG_SIM_PAIRWISE_DAG_NODE_JIANG_CONRATH_1997_NORM , icConf)


def z = 0

def counter = new AtomicInteger(0)
def alreadyDone = new ConcurrentHashMap()
def oWriter = new BufferedWriter(new FileWriter('data/create_ws_dis-dis_network/ws_dis_sim.tsv'))
def orgList = allScores.keySet().toList().collate((allScores.keySet().size()/60).intValue())

GParsPool.withPool { p ->
orgList.eachParallel { vals ->
  def engine = new SM_Engine(graph)
  vals.each { d1 ->
    def v1 = allScores[d1]
    def results = []
    println "${counter.getAndIncrement()}/${allScores.size()} ($d1)"

    allScores.each { d2, v2 ->
      if(alreadyDone.containsKey(d2)) { return; }
      def sim
      try {
        sim = engine.compare(smConfGroupwise, smConfPairwise, 
          v1.ws.collect { factory.getURI('http://purl.obolibrary.org/obo/'+it.replace(':','_')) }.findAll { graph.containsVertex(it) }.toSet(),
          v2.ws.collect { factory.getURI('http://purl.obolibrary.org/obo/'+it.replace(':','_')) }.findAll { graph.containsVertex(it) }.toSet())
      } catch(e) { 
        sim = 0 
      }

      results << [
        d1,
        d2,
        sim,
        d1 == d2
      ]
    }

    results.each {
      oWriter.write(it.join('\t') + '\n')
    }
    alreadyDone[d1] = true
  }
}
}

oWriter.flush()
oWriter.close()
