package it.unical.ea_project.service.media;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

class PhotoStorageServiceTest {
    @TempDir
    Path directory;

    MockMultipartFile png(int width) throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, 1, BufferedImage.TYPE_INT_RGB), "png", output);
        return new MockMultipartFile("file", "photo.png", "image/png", output.toByteArray());
    }

    @Test
    void rejectsOversizedHeaderBeforeDecodingAndBoundsStoredDimensions() throws Exception {
        var storage = new PhotoStorageService(directory.toString());
        byte[] bomb = png(1).getBytes();
        ByteBuffer.wrap(bomb).putInt(16, 9000);
        CRC32 crc = new CRC32();
        crc.update(bomb, 12, 17);
        ByteBuffer.wrap(bomb).putInt(29, (int) crc.getValue());
        var invalid = new MockMultipartFile("file", "bomb.png", "image/png", bomb);
        var error = assertThrows(IllegalArgumentException.class, () -> storage.validatePhoto(invalid));
        assertTrue(error.getMessage().contains("dimensioni"));
        storage.storeTripPhoto(1L, 1L, png(5000));
        assertEquals(2400, ImageIO.read(directory.resolve("trip/1/1.jpg").toFile()).getWidth());
        assertThrows(ResponseStatusException.class, () -> storage.storeTripPhoto(1L, 2L,
                new MockMultipartFile("file", "bad.png", "image/png", new byte[] { 1, 2, 3 })));
    }

    @Test
    void uploadedFilesAreRemovedOnRollbackAndDeletionWaitsForCommit() throws Exception {
        var storage = new PhotoStorageService(directory.toString());
        Path file = directory.resolve("trip/1/1.jpg");
        TransactionSynchronizationManager.initSynchronization();
        try {
            storage.storeTripPhoto(1L, 1L, png(2));
            assertTrue(Files.exists(file));
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            assertFalse(Files.exists(file));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
        storage.storeTripPhoto(1L, 1L, png(2));
        TransactionSynchronizationManager.initSynchronization();
        try {
            storage.deleteOfferPhoto("/photo/trip/1/1.jpg");
            assertTrue(Files.exists(file));
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
            assertFalse(Files.exists(file));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void avatarRollbackKeepsThePreviousPhotoAndRemovesOnlyTheNewFile() throws Exception {
        var storage = new PhotoStorageService(directory.toString());
        String original = storage.storeProfilePhoto(7L, png(2));
        Path oldFile = directory.resolve(original.substring("/photo/".length()));
        TransactionSynchronizationManager.initSynchronization();
        try {
            String changed = storage.storeProfilePhoto(7L, png(3));
            Path newFile = directory.resolve(changed.substring("/photo/".length()));
            assertNotEquals(original, changed);
            storage.deleteProfilePhoto(7L, original);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            assertTrue(Files.exists(oldFile));
            assertFalse(Files.exists(newFile));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
