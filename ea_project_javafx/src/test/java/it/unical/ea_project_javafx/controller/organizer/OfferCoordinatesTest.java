package it.unical.ea_project_javafx.controller.organizer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class OfferCoordinatesTest {
 @Test void acceptsCommaAndBoundaryCoordinates() {
  assertArrayEquals(new Double[]{39.3,16.2}, OfferCoordinates.parse("39,3","16,2"));
  assertArrayEquals(new Double[]{-90d,180d}, OfferCoordinates.parse("-90","180"));
 }
 @Test void emptyCoordinatesCanBeCleared() { assertArrayEquals(new Double[]{null,null},OfferCoordinates.parse("", "")); }
 @Test void rejectsPartialNonFiniteAndInvalidCoordinates() {
  for (String[] pair : new String[][]{{"39",""},{"91","16"},{"39","181"},{"NaN","16"},{"39","Infinity"}})
   assertThrows(IllegalArgumentException.class, () -> OfferCoordinates.parse(pair[0],pair[1]));
 }
}
