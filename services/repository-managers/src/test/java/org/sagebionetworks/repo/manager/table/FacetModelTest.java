package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.query.ParseException;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class FacetModelTest {

	Set<String> supportedFacetColumns;
	Set<String> requestedFacetColumns;

	String tableId;
	ColumnModel facetColumnModel;
	ColumnModel facetColumnModel2;
	List<ValidatedQueryFacetColumn> validatedQueryFacetColumns;
	String facetColumnName;
	String facetColumnName2;
	SqlQuery simpleQuery;
	String facetColumnId;
	String facetColumnId2;
	List<ColumnModel> facetSchema;
	Long min;
	Long max;
	String selectedValue;
	FacetColumnRangeRequest rangeRequest;
	FacetColumnValuesRequest valuesRequest;
	ArrayList<FacetColumnRequest> selectedFacets;
	
	@Before
	public void setUp() throws Exception {
		tableId = "syn123";
		supportedFacetColumns = new HashSet<>();
		requestedFacetColumns = new HashSet<>();
		validatedQueryFacetColumns = new ArrayList<>();

		facetColumnId = "890";
		facetColumnName = "asdf";
	
		facetColumnModel = new ColumnModel();
		facetColumnModel.setName(facetColumnName);
		facetColumnModel.setId(facetColumnId);
		facetColumnModel.setColumnType(ColumnType.INTEGER);
		facetColumnModel.setMaximumSize(50L);
		facetColumnModel.setFacetType(FacetType.range);
		
		facetColumnId2 = "098";
		facetColumnName2 = "qwerty";
		facetColumnModel2 = new ColumnModel();
		facetColumnModel2.setName(facetColumnName2);
		facetColumnModel2.setId(facetColumnId2);
		facetColumnModel2.setColumnType(ColumnType.STRING);
		facetColumnModel2.setMaximumSize(50L);
		facetColumnModel2.setFacetType(FacetType.enumeration);
		
		
		facetSchema = Lists.newArrayList(facetColumnModel, facetColumnModel2);
		
		selectedValue = "someValue";
		min = 23L;
		max = 56L;
		rangeRequest = new FacetColumnRangeRequest();
		rangeRequest.setColumnName(facetColumnName);
		rangeRequest.setMax(max.toString());
		rangeRequest.setMin(min.toString());
		valuesRequest = new FacetColumnValuesRequest();
		valuesRequest.setColumnName(facetColumnName2);
		valuesRequest.setFacetValues(Sets.newHashSet(selectedValue));
		
		simpleQuery = new SqlQuery("select * from " + tableId, facetSchema);
		selectedFacets = Lists.newArrayList((FacetColumnRequest)rangeRequest, (FacetColumnRequest)valuesRequest);
		
	}
	
	
	///////////////////////////////
	// createValidatedFacetsList()
	///////////////////////////////
	@Test (expected = IllegalArgumentException.class)
	public void testCreateValidatedFacetsListNullSchema(){
		boolean returnFacets = true;
		FacetModel.createValidatedFacetsList(selectedFacets , null, returnFacets);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testCreateValidatedFacetsListUnsupportedColumnName(){
		boolean returnFacets = true;
		//remove one column from schema
		facetSchema.remove(0);
		
		assertEquals(1, facetSchema.size()); //only 1 column in schema now
		assertEquals(2, selectedFacets.size()); //but filter on 2 facet columns
		
		FacetModel.createValidatedFacetsList(selectedFacets , facetSchema, returnFacets);		
	}
	
	@Test
	public void testCreateValidatedFacetsList(){
		boolean returnFacets = true;
		
		
		List<ValidatedQueryFacetColumn> result = FacetModel.createValidatedFacetsList(selectedFacets , facetSchema, returnFacets);
		
		//check that we got nonEmptyList back
		//processFacetColumnRequest tests handles case where some columns don't get added
		assertEquals(2, result.size());
		assertEquals(facetColumnName, result.get(0).getColumnName());
		assertEquals(facetColumnName2, result.get(1).getColumnName());
	}
	
	

	/////////////////////////////////////////////
	// createColumnNameToFacetColumnMap() Tests
	/////////////////////////////////////////////

	@Test
	public void testCreateColumnNameToFacetColumnMapNullList() {
		Map<String, FacetColumnRequest> map = FacetModel.createColumnNameToFacetColumnMap(null);
		assertNotNull(map);
		assertEquals(0, map.size());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateColumnNameToFacetColumnMapDuplicateName() {
		// setup
		FacetColumnRequest facetRequest1 = new FacetColumnRangeRequest();
		String sameName = facetColumnName;
		facetRequest1.setColumnName(sameName);
		FacetColumnRequest facetRequest2 = new FacetColumnRangeRequest();
		facetRequest2.setColumnName(sameName);

		FacetModel.createColumnNameToFacetColumnMap(Lists.newArrayList(facetRequest1, facetRequest2));
	}

	@Test
	public void testCreateColumnNameToFacetColumnMap() {
		Map<String, FacetColumnRequest> map = FacetModel
				.createColumnNameToFacetColumnMap(selectedFacets);
		assertNotNull(map);
		assertEquals(2, map.size());
		assertEquals(rangeRequest, map.get(facetColumnName));
		assertEquals(valuesRequest, map.get(facetColumnName2));
	}

	////////////////////////////////////////////
	// processFacetColumnRequest() tests
	////////////////////////////////////////////
	@Test
	public void testProcessFacetColumnRequestColumnModelFacetTypeIsNull() {
		facetColumnModel.setFacetType(null);

		FacetModel.processFacetColumnRequest(validatedQueryFacetColumns, supportedFacetColumns, facetColumnModel,
				new FacetColumnRangeRequest(), true);
		assertTrue(validatedQueryFacetColumns.isEmpty());
		assertTrue(supportedFacetColumns.isEmpty());
	}

	@Test
	public void testProcessFacetColumnRequestFacetParamsNullReturnFacetsFalse() {
		facetColumnModel.setFacetType(FacetType.range);

		FacetModel.processFacetColumnRequest(validatedQueryFacetColumns, supportedFacetColumns, facetColumnModel, null,
				false);
		assertTrue(validatedQueryFacetColumns.isEmpty());
		assertEquals(1, supportedFacetColumns.size());

	}

	@Test
	public void testProcessFacetColumnRequestFacetParamsNullReturnFacetsTrue() {
		facetColumnModel.setFacetType(FacetType.range);

		FacetModel.processFacetColumnRequest(validatedQueryFacetColumns, supportedFacetColumns, facetColumnModel, null,
				true);
		assertEquals(1, validatedQueryFacetColumns.size());
		ValidatedQueryFacetColumn validatedQueryFacetColumn = validatedQueryFacetColumns.get(0);
		assertNull(validatedQueryFacetColumn.getFacetColumnRequest());
		assertEquals(facetColumnName, validatedQueryFacetColumn.getColumnName());
		assertEquals(FacetType.range, validatedQueryFacetColumn.getFacetType());
		assertEquals(1, supportedFacetColumns.size());

	}

	@Test
	public void testProcessFacetColumnRequestFacetParamsNotNull() {
		// setup
		facetColumnModel.setFacetType(FacetType.range);

		FacetColumnRangeRequest facetRange = new FacetColumnRangeRequest();
		facetRange.setMin("123");
		facetRange.setMax("456");
		facetRange.setColumnName(facetColumnName);

		FacetModel.processFacetColumnRequest(validatedQueryFacetColumns, supportedFacetColumns, facetColumnModel,
				facetRange, false);

		assertEquals(1, validatedQueryFacetColumns.size());
		ValidatedQueryFacetColumn validatedQueryFacetColumn = validatedQueryFacetColumns.get(0);
		assertEquals(facetRange, validatedQueryFacetColumn.getFacetColumnRequest());
		assertEquals(facetColumnName, validatedQueryFacetColumn.getColumnName());
		assertEquals(FacetType.range, validatedQueryFacetColumn.getFacetType());
		assertEquals(1, supportedFacetColumns.size());
	}
	///////////////////////////////////////
	// generateFacetFilteredQuery() Tests
	///////////////////////////////////////
	@Test (expected = IllegalArgumentException.class)
	public void testGenerateFacetFilteredQueryNullQuery() {
		FacetModel.generateFacetFilteredQuery(null, validatedQueryFacetColumns);
	}
	@Test (expected = IllegalArgumentException.class)
	public void testGenerateFacetFilteredQueryNullList() {
		FacetModel.generateFacetFilteredQuery(simpleQuery, null);
	}
	
	@Test
	public void testGenerateFacetFilteredQueryEmptyFacetColumnsList() throws ParseException {
		assertTrue(validatedQueryFacetColumns.isEmpty());
		SqlQuery copy = FacetModel.generateFacetFilteredQuery(simpleQuery, validatedQueryFacetColumns);
		// not same reference, are different object instances
		assertTrue(simpleQuery != copy);
		// but are essentially the same
		assertEquals(simpleQuery.getOutputSQL(), copy.getOutputSQL());

	}

	@Test
	public void testGenerateFacetFilteredQueryNonEmptyFacetColumnsList() throws ParseException {
		SqlQuery query = new SqlQuery("select * from " + tableId + " where asdf <> ayy and asdf < 'taco bell'",
				facetSchema);

		validatedQueryFacetColumns.add(new ValidatedQueryFacetColumn(facetColumnName, FacetType.range, rangeRequest));

		SqlQuery modifiedQuery = FacetModel.generateFacetFilteredQuery(query, validatedQueryFacetColumns);
		assertEquals("SELECT _C" + facetColumnId + "_, _C" + facetColumnId2+"_, ROW_ID, ROW_VERSION FROM T" + KeyFactory.stringToKey(tableId)
				+ " WHERE ( _C" + facetColumnId + "_ <> :b0 AND _C" + facetColumnId + "_ < :b1 ) AND ( ( ( _C"
				+ facetColumnId + "_ BETWEEN :b2 AND :b3 ) ) )", modifiedQuery.getOutputSQL());
		assertEquals(min, modifiedQuery.getParameters().get("b2"));
		assertEquals(max, modifiedQuery.getParameters().get("b3"));
	}
	
	///////////////////////////////////////////
	// generateFacetQueryTransformers() tests
	///////////////////////////////////////////
	@Test (expected = IllegalArgumentException.class)
	public void testGenerateFacetQueryTransformersNullQuery() {
		FacetModel.generateFacetQueryTransformers(null, validatedQueryFacetColumns);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testGenerateFacetQueryTransformersNullList() {
		FacetModel.generateFacetQueryTransformers(simpleQuery, null);
	}
	
	@Test
	public void testGenerateFacetQueryTransformers(){
		validatedQueryFacetColumns.add(new ValidatedQueryFacetColumn(facetColumnName, FacetType.range, rangeRequest));
		validatedQueryFacetColumns.add(new ValidatedQueryFacetColumn(facetColumnName2, FacetType.enumeration, valuesRequest));
		
		List<FacetTransformer> result = FacetModel.generateFacetQueryTransformers(simpleQuery, validatedQueryFacetColumns);
		//just check for the correct item types.  
		//the transformers' unit tests already check that fields are set correctly
		assertEquals(2, result.size());
		assertTrue(result.get(0) instanceof FacetTransformerRange);
		assertTrue(result.get(1) instanceof FacetTransformerValueCounts);
	}
	
}
