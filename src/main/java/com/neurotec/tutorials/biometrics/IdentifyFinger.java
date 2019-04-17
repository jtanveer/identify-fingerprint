package com.neurotec.tutorials.biometrics;

import java.io.IOException;
import java.util.EnumSet;

import com.neurotec.biometrics.*;
import com.neurotec.biometrics.client.NBiometricClient;
import com.neurotec.io.NBuffer;
import com.neurotec.io.NFile;
import com.neurotec.licensing.NLicense;
import com.neurotec.licensing.NLicenseManager;
import com.neurotec.plugins.NDataFileManager;
import com.neurotec.tutorials.util.LibraryManager;
import com.neurotec.tutorials.util.Utils;

public final class IdentifyFinger {
	private static final String DESCRIPTION = "Demonstrates fingerprint identification";
	private static final String NAME = "identify-finger";
	private static final String VERSION = "11.0.0.0";

	private static void usage() {
		System.out.println("usage:");
		System.out.format("\t%s [probe image] [one or more gallery images]%n", NAME);
		System.out.println();
	}

	public static void main(String[] args) {
		LibraryManager.initLibraryPath();

		Utils.printTutorialHeader(DESCRIPTION, NAME, VERSION, args);

//		if (args.length < 2) {
//			usage();
//			System.exit(1);
//		}

		//=========================================================================
		// CHOOSE LICENCES !!!
		//=========================================================================
		// ONE of the below listed "licenses" lines is required for unlocking this sample's functionality. Choose licenses that you currently have on your device.
		// If you are using a TRIAL version - choose any of them.

//		final String licenses = "FingerMatcher,FingerExtractor"; // VeriFinger Standard
		final String licenses = "FingerMatcher,FingerClient"; // Verifinger Extended
//		final String licenses = "FingerFastMatcher,FingerFastExtractor"; // MegaMatcher Standard

		//=========================================================================

		//=========================================================================
		// TRIAL MODE
		//=========================================================================
		// Below code line determines whether TRIAL is enabled or not. To use purchased licenses, don't use below code line.
		// GetTrialModeFlag() method takes value from "Bin/Licenses/TrialFlag.txt" file. So to easily change mode for all our examples, modify that file.
		// Also you can just set TRUE to "TrialMode" property in code.
		//=========================================================================

		try {
			boolean trialMode = Utils.getTrialModeFlag();
			NLicenseManager.setTrialMode(trialMode);
			System.out.println("\tTrial mode: " + trialMode);
		} catch (IOException e) {
			e.printStackTrace();
		}

//		NDataFileManager.getInstance().addFromDirectory("C:\\Users\\jamael\\Downloads\\Neurotec_Biometric_11_0_SDK\\Bin\\Data", false);
		NDataFileManager.getInstance().addFile("Fingers.ndf");

		NBiometricClient biometricClient = null;
		NSubject probeSubject = null;
		NBiometricTask enrollTask = null;
		
		try {
			// Obtain licenses
			if (!NLicense.obtain("/local", 5000, licenses)) {
				System.err.format("Could not obtain licenses: %s%n", licenses);
				System.exit(-1);
			}

			System.out.println("Working Directory = " +
					System.getProperty("user.dir"));

//			long start = System.currentTimeMillis();
//			System.out.println("start time: " + start);

			biometricClient = new NBiometricClient();
			probeSubject = createSubject("fingerprint_7_2.jpg", "ProbeSubject");

			NBiometricStatus status = biometricClient.createTemplate(probeSubject);

			if (status != NBiometricStatus.OK) {
				System.out.format("Failed to create probe template. Status: %s.\n", status);
				System.exit(-1);
			}

			long start = System.currentTimeMillis();
			System.out.println("start time: " + start);

			enrollTask = biometricClient.createTask(EnumSet.of(NBiometricOperation.ENROLL), null);
			NBuffer buffer = NFile.readAllBytes("template");
			NTemplate template = new NTemplate(buffer);
			for (int i = 0; i < 10; i++) {
				System.out.println("enrolling fingerprint: " + (i + 1));
				// enroll from template
				enrollTask.getSubjects().add(createSubject(template, String.format("GallerySubject_%d", i)));
				// enroll from image
//				NSubject candidateSubject = createSubject("fingerprint_3.wsq", String.format("GallerySubject_%d", i));
//				enrollTask.getSubjects().add(candidateSubject);
			}
			biometricClient.performTask(enrollTask);

			long end = System.currentTimeMillis();
			System.out.println("end time: " + end);
			System.out.println("time taken for extraction: " + (end - start));

			if (enrollTask.getStatus() != NBiometricStatus.OK) {
				System.out.format("Enrollment was unsuccessful. Status: %s.\n", enrollTask.getStatus());
				if (enrollTask.getError() != null) throw enrollTask.getError();
				System.exit(-1);
			}

			System.out.println("size: " + biometricClient.getFingersTemplateSize().getValue());

			biometricClient.setMatchingThreshold(48);

			biometricClient.setFingersMatchingSpeed(NMatchingSpeed.LOW);

			start = System.currentTimeMillis();
			System.out.println("start time: " + start);

			status = biometricClient.identify(probeSubject);

			end = System.currentTimeMillis();
			System.out.println("end time: " + end);
			System.out.println("time taken for identification: " + (end - start));

			if (status == NBiometricStatus.OK) {
				for (NMatchingResult result : probeSubject.getMatchingResults()) {
					System.out.format("Matched with ID: '%s' with score %d\n", result.getId(), result.getScore());
				}
			} else if (status == NBiometricStatus.MATCH_NOT_FOUND) {
				System.out.format("Match not found");
			} else {
				System.out.format("Identification failed. Status: %s\n", status);
				System.exit(-1);
			}
//			long end = System.currentTimeMillis();
//			System.out.println("end time: " + end);
//			System.out.println("time taken: " + (end - start));
		} catch (Throwable th) {
			Utils.handleError(th);
		} finally {
			if (enrollTask != null) enrollTask.dispose();
			if (probeSubject != null) probeSubject.dispose();
			if (biometricClient != null) biometricClient.dispose();
		}
	}

	private static NSubject createSubject(String fileName, String subjectId) {
		NSubject subject = new NSubject();
		NFinger finger = new NFinger();
		finger.setFileName(fileName);
		subject.getFingers().add(finger);
		subject.setId(subjectId);
		return subject;
	}

	private static NSubject createSubject(NTemplate template, String id) {
		NSubject subject = new NSubject();
		subject.setTemplate(template);
		subject.setId(id);
		return subject;
	}

}
