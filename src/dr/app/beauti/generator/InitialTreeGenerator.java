package dr.app.beauti.generator;

import dr.app.beauti.components.ComponentFactory;
import dr.app.beauti.enumTypes.FixRateType;
import dr.app.beauti.enumTypes.PriorType;
import dr.app.beauti.enumTypes.TreePriorType;
import dr.app.beauti.options.BeautiOptions;
import dr.app.beauti.options.Parameter;
import dr.app.beauti.options.PartitionTreeModel;
import dr.app.beauti.options.PartitionTreePrior;
import dr.app.beauti.util.XMLWriter;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evolution.util.Taxa;
import dr.evomodelxml.coalescent.CoalescentSimulatorParser;
import dr.evomodelxml.coalescent.ConstantPopulationModelParser;
import dr.evomodelxml.coalescent.ExponentialGrowthModelParser;
import dr.evoxml.*;
import dr.inference.distribution.UniformDistributionModel;
import dr.util.Attribute;
import dr.xml.XMLParser;

/**
 * @author Alexei Drummond
 * @author Walter Xie
 */
public class InitialTreeGenerator extends Generator {
    final static public String STARTING_TREE = "startingTree";

    public InitialTreeGenerator(BeautiOptions options, ComponentFactory[] components) {
        super(options, components);
    }

    /**
     * Generate XML for the starting tree
     * @param model  PartitionTreeModel 
     *
     * @param writer the writer
     */
    public void writeStartingTree(PartitionTreeModel model, XMLWriter writer) {
    	
    	setModelPrefix(model.getPrefix()); // only has prefix, if (options.getPartitionTreeModels().size() > 1) 
    	
        Parameter rootHeight = model.getParameter("treeModel.rootHeight");
        
        switch (model.getStartingTreeType()) {
            case USER:
                writeUserTree(model.getUserStartingTree(), writer);
                break;

            case UPGMA:
                // generate a upgma starting tree
                writer.writeComment("Construct a rough-and-ready UPGMA tree as an starting tree");
                if (rootHeight.priorType != PriorType.NONE) {
                    writer.writeOpenTag(
                            UPGMATreeParser.UPGMA_TREE,
                            new Attribute[]{
                                    new Attribute.Default<String>(XMLParser.ID, modelPrefix + STARTING_TREE),
                                    new Attribute.Default<String>(UPGMATreeParser.ROOT_HEIGHT, "" + rootHeight.initial)
                            }
                    );
                } else {
                    writer.writeOpenTag(
                            UPGMATreeParser.UPGMA_TREE,
                            new Attribute[]{
                                    new Attribute.Default<String>(XMLParser.ID, modelPrefix + STARTING_TREE)
                            }
                    );
                }
                writer.writeOpenTag(
                        DistanceMatrixParser.DISTANCE_MATRIX,
                        new Attribute[]{
                                new Attribute.Default<String>(DistanceMatrixParser.CORRECTION, "JC")
                        }
                );
                writer.writeOpenTag(SitePatternsParser.PATTERNS);
                writer.writeComment("To generate UPGMA starting tree, only use the 1st aligment, "
                        + "which may be 1 of many aligments using this tree.");
                writer.writeIDref(AlignmentParser.ALIGNMENT, model.getAllPartitionData().get(0).getAlignment().getId());
                // alignment has no gene prefix
                writer.writeCloseTag(SitePatternsParser.PATTERNS);
                writer.writeCloseTag(DistanceMatrixParser.DISTANCE_MATRIX);
                writer.writeCloseTag(UPGMATreeParser.UPGMA_TREE);
                break;

            case RANDOM:
                // generate a coalescent tree
                writer.writeComment("Generate a random starting tree under the coalescent process");
                               	
                if (options.clockModelOptions.getRateOptionClockModel() == FixRateType.FIX_MEAN
            			|| options.clockModelOptions.getRateOptionClockModel() == FixRateType.RELATIVE_TO) {            	
                	
            		writer.writeComment("No calibration");
	            	writer.writeOpenTag(
	                        CoalescentSimulatorParser.COALESCENT_TREE,
	                        new Attribute[]{
	                                new Attribute.Default<String>(XMLParser.ID, modelPrefix + STARTING_TREE),
	                                new Attribute.Default<String>(CoalescentSimulatorParser.ROOT_HEIGHT, "" + rootHeight.initial)
	                        }
	                );

                } else {
            		writer.writeComment("Has calibration");
            		writer.writeOpenTag(
	                        CoalescentSimulatorParser.COALESCENT_TREE,
	                        new Attribute[]{
	                                new Attribute.Default<String>(XMLParser.ID, modelPrefix + STARTING_TREE)
	                        }
	                );
            	}
                
                String taxaId;
                if (options.allowDifferentTaxa) {//BEAST cannot handle multi <taxa> ref for 1 tree
                    if (model.getAllPartitionData().size() > 1) {
                        if (!options.validateDiffTaxa(model.getAllPartitionData())) {
                            throw new IllegalArgumentException("To allow different taxa, each taxa has to have a tree model !");
                        }
                    }

//                    for (PartitionData partition : model.getAllPartitionData()) {
//                        taxaId = partition.getPrefix() + TaxaParser.TAXA;
//                        writeTaxaRef(taxaId, model, writer);
//                        break; //only need 1 taxa ref
//                    }
                    taxaId = model.getAllPartitionData().get(0).getPrefix() + TaxaParser.TAXA;
                    writeTaxaRef(taxaId, model, writer);
                    
                } else {
                	taxaId = TaxaParser.TAXA;
                	writeTaxaRef(taxaId, model, writer);
                }

                writeInitialDemoModelRef(model, writer);
                writer.writeCloseTag(CoalescentSimulatorParser.COALESCENT_TREE);
                break;
            default:
                throw new IllegalArgumentException("Unknown StartingTreeType");

        }
    }
    
    private void writeTaxaRef(String taxaId, PartitionTreeModel model, XMLWriter writer) {
    	
        Attribute[] taxaAttribute = {new Attribute.Default<String>(XMLParser.IDREF, taxaId)};
        
        if (options.taxonSets != null && options.taxonSets.size() > 0) { 
            writer.writeOpenTag(CoalescentSimulatorParser.CONSTRAINED_TAXA);
            writer.writeTag(TaxaParser.TAXA, taxaAttribute, true);
            for (Taxa taxa : options.taxonSets) {
                if (taxa.getTreeModel().equals(model)) {
                Parameter statistic = options.getStatistic(taxa);

                Attribute mono = new Attribute.Default<Boolean>(
                        CoalescentSimulatorParser.IS_MONOPHYLETIC, options.taxonSetsMono.get(taxa));

                writer.writeOpenTag(CoalescentSimulatorParser.TMRCA_CONSTRAINT, mono);

                writer.writeIDref(TaxaParser.TAXA, taxa.getId());
                if (statistic.isNodeHeight) {
                    if (statistic.priorType == PriorType.UNIFORM_PRIOR || statistic.priorType == PriorType.TRUNC_NORMAL_PRIOR) {
                        writer.writeOpenTag(UniformDistributionModel.UNIFORM_DISTRIBUTION_MODEL);
                        writer.writeTag(UniformDistributionModel.LOWER, new Attribute[]{}, "" + statistic.lower, true);
                        writer.writeTag(UniformDistributionModel.UPPER, new Attribute[]{}, "" + statistic.upper, true);
                        writer.writeCloseTag(UniformDistributionModel.UNIFORM_DISTRIBUTION_MODEL);
                    }
                }

                writer.writeCloseTag(CoalescentSimulatorParser.TMRCA_CONSTRAINT);
                }
            }
            writer.writeCloseTag(CoalescentSimulatorParser.CONSTRAINED_TAXA);
        } else {
            writer.writeTag(TaxaParser.TAXA, taxaAttribute, true);
        }
    }

    private void writeInitialDemoModelRef(PartitionTreeModel model, XMLWriter writer) {
    	PartitionTreePrior prior = model.getPartitionTreePrior();
    		
		if (prior.getNodeHeightPrior() == TreePriorType.CONSTANT || options.starBEASTOptions.isSpeciesAnalysis()) {
        	writer.writeIDref(ConstantPopulationModelParser.CONSTANT_POPULATION_MODEL, prior.getPrefix() + "constant");
        } else if (prior.getNodeHeightPrior() == TreePriorType.EXPONENTIAL) {
        	writer.writeIDref(ExponentialGrowthModelParser.EXPONENTIAL_GROWTH_MODEL, prior.getPrefix() + "exponential");
        } else {
        	writer.writeIDref(ConstantPopulationModelParser.CONSTANT_POPULATION_MODEL, prior.getPrefix() + "initialDemo");
        }     		    		
    	
    }

    /**
     * Generate XML for the user tree
     *
     * @param tree   the user tree
     * @param writer the writer
     */
    private void writeUserTree(Tree tree, XMLWriter writer) {

        writer.writeComment("The starting tree.");
        writer.writeOpenTag(
                "tree",
                new Attribute[]{
                        new Attribute.Default<String>("height", STARTING_TREE),
                        new Attribute.Default<String>("usingDates", (options.clockModelOptions.isTipCalibrated() ? "true" : "false"))
                }
        );
        writeNode(tree, tree.getRoot(), writer);
        writer.writeCloseTag("tree");
    }

    /**
     * Generate XML for the node of a user tree.
     *
     * @param tree   the user tree
     * @param node   the current node
     * @param writer the writer
     */
    private void writeNode(Tree tree, NodeRef node, XMLWriter writer) {

        writer.writeOpenTag(
                "node",
                new Attribute[]{new Attribute.Default<String>("height", "" + tree.getNodeHeight(node))}
        );

        if (tree.getChildCount(node) == 0) {
        	writer.writeIDref(TaxonParser.TAXON, tree.getNodeTaxon(node).getId());
        }
        for (int i = 0; i < tree.getChildCount(node); i++) {
            writeNode(tree, tree.getChild(node, i), writer);
        }
        writer.writeCloseTag("node");
    }
}
