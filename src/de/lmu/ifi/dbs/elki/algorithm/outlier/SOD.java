package de.lmu.ifi.dbs.elki.algorithm.outlier;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.result.outlier.SODModel;
import de.lmu.ifi.dbs.elki.algorithm.result.outlier.SODResult;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SharedNearestNeighborSimilarityFunction;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.KNNList;
import de.lmu.ifi.dbs.elki.utilities.Progress;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.Iterator;
import java.util.List;

/**
 * @author Arthur Zimek
 * @param <<V> the type of RealVector handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */
// todo arthur comment
public class SOD<V extends RealVector<V, Double>, D extends Distance<D>> extends AbstractAlgorithm<V> {

    /**
     * The association id to associate a subspace outlier degree.
     */
    @SuppressWarnings("unchecked")
    public static final AssociationID<SODModel> SOD_MODEL = AssociationID.getOrCreateAssociationID("SOD", SODModel.class);

    /**
     * OptionID for {@link #KNN_PARAM}
     */
    public static final OptionID KNN_ID = OptionID.getOrCreateOptionID(
        "sod.knn",
        "The number of shared nearest neighbors to be considered for learning the subspace properties."
    );

    /**
     * Parameter to specify the number of shared nearest neighbors to be considered for learning the subspace properties.,
     * must be an integer greater than 0.
     * <p>Default value: {@code 1} </p>
     * <p>Key: {@code -sod.knn} </p>
     */
    private final IntParameter KNN_PARAM = new IntParameter(KNN_ID, new GreaterConstraint(0), 1);

    /**
     * Holds the value of {@link #KNN_PARAM}.
     */
    private int knn;

    /**
     * OptionID for {@link #ALPHA_PARAM}
     */
    public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID(
        "sod.alpha",
        "The multiplier for the discriminance value for discerning small from large variances."
    );

    /**
     * Parameter to indicate the multiplier for the discriminance value for discerning small from large variances.
     * <p/>
     * <p>Default value: 1.1</p>
     * <p/>
     * <p>Key: {@code -sod.alpha}</p>
     */
    public final DoubleParameter ALPHA_PARAM = new DoubleParameter(ALPHA_ID, new GreaterConstraint(0), 1.1);

    /**
     * Holds the value of {@link #ALPHA_PARAM}.
     */
    private double alpha;

    /**
     * The similarity function.
     */
    private SharedNearestNeighborSimilarityFunction<V, D> similarityFunction = new SharedNearestNeighborSimilarityFunction<V, D>();

    /**
     * Holds the result.
     */
    private SODResult<V> sodResult;

    /**
     * Provides the SOD algorithm,
     * adding parameters
     * {@link #KNN_PARAM} and {@link #ALPHA_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public SOD() {
        super();
        addOption(KNN_PARAM);
        addOption(ALPHA_PARAM);
    }

    /**
     * Calls {@link AbstractAlgorithm#parameterDescription()}
     * and appends the parameter description of {@link #similarityFunction}.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#parameterDescription()
     */
    @Override
    public String parameterDescription() {
        StringBuilder description = new StringBuilder();
        description.append(super.parameterDescription());

        // similarityFunction
        description.append(Description.NEWLINE);
        description.append(similarityFunction.parameterDescription());

        return description.toString();
    }

    /**
     * Performs the PROCLUS algorithm on the given database.
     *
     * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#run(de.lmu.ifi.dbs.elki.database.Database)
     */
    protected void runInTime(Database<V> database) throws IllegalStateException {
        Progress progress = new Progress("assigning SOD", database.size());
        int processed = 0;
        similarityFunction.setDatabase(database, isVerbose(), isTime());
        if (isVerbose()) {
            verbose("assigning subspace outlier degree:");
        }
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
            Integer queryObject = iter.next();
            processed++;
            if (isVerbose()) {
                progress.setProcessed(processed);
                progress(progress);
            }
            List<Integer> knnList = getKNN(database, queryObject).idsToList();
            SODModel<V> model = new SODModel<V>(database, knnList, alpha, database.get(queryObject));
            database.associate(SOD_MODEL, queryObject, model);
        }
        if (isVerbose()) {
            verbose("");
        }
        sodResult = new SODResult<V>(database);
    }

    /**
     * Provides the k nearest neighbors in terms of the shared nearest neighbor distance.
     * <p/>
     * The query object is excluded from the knn list.
     *
     * @param database    the database holding the objects
     * @param queryObject the query object for which the kNNs should be determined
     * @return the k nearest neighbors in terms of the shared nearest neighbor distance without the query object
     */
    private KNNList<DoubleDistance> getKNN(Database<V> database, Integer queryObject) {
        similarityFunction.getPreprocessor().getParameters();
        KNNList<DoubleDistance> kNearestNeighbors = new KNNList<DoubleDistance>(knn, new DoubleDistance(Double.POSITIVE_INFINITY));
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
            Integer id = iter.next();
            if (!id.equals(queryObject)) {
                DoubleDistance distance = new DoubleDistance(1.0 / similarityFunction.similarity(queryObject, id).getValue());
                kNearestNeighbors.add(new QueryResult<DoubleDistance>(id, distance));
            }
        }
        return kNearestNeighbors;
    }

    /**
     * Calls {@link de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm#setParameters(String[]) AbstractAlgorithm#setParameters(args)}
     * and sets additionally the values of the parameters
     * {@link #KNN_PARAM} and {@link #ALPHA_PARAM}.
     * The remaining parameters are passed to the {@link #similarityFunction}.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);
        knn = getParameterValue(KNN_PARAM);
        alpha = getParameterValue(ALPHA_PARAM);

        remainingParameters = similarityFunction.setParameters(remainingParameters);
        setParameters(args, remainingParameters);
        return remainingParameters;
    }

    /**
     * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#getDescription()
     */
    public Description getDescription() {
        return new Description("SOD", "Subspace outlier degree", "", "");
    }

    /**
     * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#getResult() ()
     */
    public SODResult<V> getResult() {
        return sodResult;
    }

    /**
     * Calls {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable#getAttributeSettings()}
     * and adds to the returned attribute settings the attribute settings of
     * the {@link #similarityFunction}.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#getAttributeSettings()
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        attributeSettings.addAll(similarityFunction.getAttributeSettings());
        return attributeSettings;
    }
}
