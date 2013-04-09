package edu.washington.cse.se.speculation.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import edu.washington.cse.se.speculation.scm.VCSGraph;
import edu.washington.cse.se.speculation.scm.VCSNode;
import edu.washington.cse.se.speculation.scm.git.VCSNodePair;

public class GitPerlConflictTester {

	public static String[][] gitConflicts = { { "0226bbdb9567884ccd3573b0b35272e596fbceba", "dd9bbc5bda8435080ddb6646df882a81445df0c3" },
			{ "07160069ef57a7888de8064b883bc0f5cbd24dc2", "cc1c1d2cf91c7eb921f74f7e5553fd9670d0e964" },
			{ "5d20095f2ea76fe4a137473b5027adb54c562036", "8e93eb227d6a2e75d395bc74dbcd4957180a1a72" },
			{ "9d45c60a0f69fc7753c5abe44596669f2f1b19ef", "14d89041d8ff788f94621c8fcb0918e3be8f01f0" },
			{ "811b48f27d947e93c43db1c396dd6128aaf61016", "14d89041d8ff788f94621c8fcb0918e3be8f01f0" },
			{ "65fe0b2a37b1f4886be0de28cfdc2bcaf5ed0771", "fe6a355d0e248c42e1da4becfeb3d3c37cc36990" },
			{ "147da4bcaa923ab664686ca9d06ab0971349e03a", "7cf2ce72097a199da7164901591cd9dabfaddef6" },
			{ "3eb7d038374ee240e9b4fbfd51cf0d5698160a53", "6b490b4c5a72d946db8698910845b81e25ef475b" },
			{ "3eb7d038374ee240e9b4fbfd51cf0d5698160a53", "6b490b4c5a72d946db8698910845b81e25ef475b" },
			{ "e55ac0fa2de97f387f14e72ecb12931a1e26d2c6", "abc0a0153433fe6596e1ca3a6b5572dc424d0f11" },
			{ "3c26928f626c1daf0fdc42cb4620bf258d47a018", "dcbc17cb9ecf341e358d03311468a8d1c6ecc444" },
			{ "cfc8a8024406f6fb9714bbb00f3199af53485a87", "54f3bd6bfff108b3d388b542cab121e33bfdea6a" },
			{ "e87ef8c0197c05592a0b7988f7f1af2f39f06465", "83d057904fcf43ccbeee0b8e23d13ba528a6cb6a" },
			{ "cb5fdd89057107323f000050b37f8574253e2073", "478d3074309ff514cb3d637215f7368927a0a9d6" },
			{ "e02aff1787731eb0d62d3feb2940a906c15312df", "294f22c5932794232f480d4b97d1291de8b3ab24" },
			{ "1d86a7f9aafa7b00ab187ace80f468664c66c924", "bf02d38ec7892c236ea4b293274ea6a3c334a25d" },
			{ "287a822c675abc6308357941862beda584b38a68", "bf02d38ec7892c236ea4b293274ea6a3c334a25d" },
			{ "3e4a31ba50c4f768e53207843155793ca70b3ba1", "498d7dc446852be2217fba65d8c41a0242e7402a" },
			{ "0202f2d889312a2f8a1479686ebbafc67572bb36", "6e6ca6c3d8f9eaf6e2364f1fef07b3f472f0a846" },
			{ "eb7e197b44fe0299233d423ce8184ba0cffc3883", "636cdfa52826c133f89802fd4f8d0dabbec733b9" },
			{ "9217ff3fe3ec1aad7b12d79198ffb9252d6908de", "020b9078de2cdc245cc99df621722187e7680145" },
			{ "0678cb22c235366e6443f8ba36afc299093d457c", "dd27d36fd41b06f247074c949c2478cdbba72468" },
			{ "f2b1176c042dd0d792a0f916f759bd848210d4d8", "67859229b2af3ba90e55d42060c363e9c83849bf" },
			{ "6b42d12b8135b62734b087ee4b02cf28d259a483", "9837d3731bea1e0d3aaed58a46127574f76ffe53" },
			{ "7777929a1344ae3adddfcf7a297005c278038c36", "d7aa4417de5aebb1d2374018c62148692ef32d8b" },
			{ "0107644769df870b4b9b5d39c23e022645709de5", "210e727c30fedfe36428f457a5e2f9e6176680c4" },
			{ "a6dd04483cb30505dd31342b8e0d790b3545179d", "23f519f064273e3cc195f02d4e0e49cf7c9329a8" },
			{ "48dc1ee65779dbdc177bc16530afd4f43c1c0ecf", "9cb315afb580ee4bef44ec55c62fba291b50e04b" },
			{ "9d1c1517f49452038dfeb57497c491f93b1616c0", "1efaba7f56949957e2b13a8f2a59502184204c6d" },
			{ "9d40e17d44e193330835b996e62e268e9aa05d1b", "7d8e7db38dc74a9a7ddcc48566f03f2b6af6f737" },
			{ "23340c0f002bdb71a989789b894e159f81453bff", "7d8e7db38dc74a9a7ddcc48566f03f2b6af6f737" },
			{ "4adc95e616bac7eea015e9e47e439b063c1132d5", "2bbc8d558d247c6ef91207a12a4650c0bc292dd6" },
			{ "b16c2e4a254d31480561f2bca5aeaeb75328de9c", "4e32d3ada00dc86f30145cd90e757baad18afd5f" },
			{ "a2f19a19a2a93bd06c4d12944dde16be09025088", "37930f0f2f3c60737f8ce994bd695a224792b7e2" },
			{ "3dac15a5e55ca29c7a6db0641c9887b1384cb5fd", "c24e7b67b597eaa15fbaca34ebb57a7cc242d937" },
			{ "036d8bd42e6e430b0e760ef10edcff408b0b2527", "c8269701e1f9e7b1dcbe771b4e7a802f050436d4" },
			{ "4411113f31b3f00171bb335092b02104d29d7cd7", "0e985b6b1215714dc84881a833eeadc5ef85c455" },
			{ "289d21b27f6ed6d510e19182f97e90e2203be76c", "f7bbabd3deb33ca111eb6b17f0252ad07f079f16" },
			{ "4f187fc91707286d3f695e2ea5e34351c2516fc6", "9091a618773bfbb9dc94067f69bea900a2c09c1d" },
			{ "933981904d595f4bab92e904b4d426ca07c8ccfc", "5a33cafb3e9fc5707648ad1389fba19eefaf2247" },
			{ "9a8a8154e760831a634ef0df130347674ea26459", "54f7170d626da3cbbd288fe2b7c7dbb547cb80e8" },
			{ "b73790d3c9fb2026c569344bfe353223af9bdbea", "fa13de9435b2f7be1099b9202ae6e26021b00ba8" },
			{ "5bc8648cf6ac025ca43fa76c724554b043d49ab6", "0e945d0082cb70df3b6da49ba9241db5ee15f208" },
			{ "9a7d94ef19fc170c3cac472a31f467828efc1cd4", "532217f11892796c16c46ab116e3d28b314a2e17" },
			{ "2b4f847434de297901e2c76921bae9eecb9a8929", "ff2f7ef032add258929ac5ef7e2cb3680b318700" },
			{ "2b4f847434de297901e2c76921bae9eecb9a8929", "f1ce3bf1be19f4a550c630992f15e96258801164" },
			{ "9c955c4d43e6b85fa829444681b37167f0dea301", "423174046424f87a68e7227cd8015f1926fede2d" },
			{ "eb78bf8f0da016f321bb20da1ca06461c3356b39", "b953482e2d970eeb88de96a38c087d03db83a5cd" },
			{ "124653e52746add2710fcb1ef598fd0ee283007b", "503b513ef40d22a4c2b669896bfe16079d756b69" },
			{ "0aeed6507963171d23ad85fa5175ab17feb9a951", "20ed56d5abd6d5f12a39534299b7fbba1916769c" },
			{ "38ec24b4509c45dcd56747fd58a17bbb8960a9a9", "07962d9a9be57321e306c86241903a78862dad8e" },
			{ "8e3247042c02fd16ad4031fefd53fa013b28bc29", "e92f586bd3fd45336a351e251027401d8d4a7135" },
			{ "9e8c01f558a03902ff2f54935fd7e6dcc7ec656c", "2e0eeeaafce11cb0128a6d1e245f1a5b806e3a87" },
			{ "5ce5779215ef1e2ae554b5ea5a8c90b62c0729aa", "51393fc07355ffd0a4b6b212fd676ee37de23e09" },
			{ "a9a5a0dc04939697af9865334c538bca556f5472", "23f0c133b39568de35784182a730cb2929c27e34" },
			{ "5b3114678323b284f88ba8d2da3cad315a53ed6e", "19185491d7b98ad254c08bed49440bbf4e185376" },
			{ "b9627ca0e5018428a0c9716cd6161b63cbc42db8", "4656de94bd0fe9964273dadbb6ad728a076eb4ee" },
			{ "89dbd0d1c0057e09d404ffc2731fbe3d87feea11", "51f494ccbae191b4b6fd4232895ccf47ceb713e5" } };

	public static String[][] perlConflicts = { { "dd9bbc5bda8435080ddb6646df882a81445df0c3", "0226bbdb9567884ccd3573b0b35272e596fbceba" },
			{ "cc1c1d2cf91c7eb921f74f7e5553fd9670d0e964", "07160069ef57a7888de8064b883bc0f5cbd24dc2" },
			{ "5d20095f2ea76fe4a137473b5027adb54c562036", "8e93eb227d6a2e75d395bc74dbcd4957180a1a72" },
			{ "14d89041d8ff788f94621c8fcb0918e3be8f01f0", "9d45c60a0f69fc7753c5abe44596669f2f1b19ef" },
			{ "14d89041d8ff788f94621c8fcb0918e3be8f01f0", "811b48f27d947e93c43db1c396dd6128aaf61016" },
			{ "fe6a355d0e248c42e1da4becfeb3d3c37cc36990", "65fe0b2a37b1f4886be0de28cfdc2bcaf5ed0771" },
			{ "7cf2ce72097a199da7164901591cd9dabfaddef6", "147da4bcaa923ab664686ca9d06ab0971349e03a" },
			{ "6b490b4c5a72d946db8698910845b81e25ef475b", "3eb7d038374ee240e9b4fbfd51cf0d5698160a53" },
			{ "6b490b4c5a72d946db8698910845b81e25ef475b", "3eb7d038374ee240e9b4fbfd51cf0d5698160a53" },
			{ "e55ac0fa2de97f387f14e72ecb12931a1e26d2c6", "abc0a0153433fe6596e1ca3a6b5572dc424d0f11" },
			{ "dcbc17cb9ecf341e358d03311468a8d1c6ecc444", "3c26928f626c1daf0fdc42cb4620bf258d47a018" },
			{ "cfc8a8024406f6fb9714bbb00f3199af53485a87", "54f3bd6bfff108b3d388b542cab121e33bfdea6a" },
			{ "83d057904fcf43ccbeee0b8e23d13ba528a6cb6a", "e87ef8c0197c05592a0b7988f7f1af2f39f06465" },
			{ "cb5fdd89057107323f000050b37f8574253e2073", "478d3074309ff514cb3d637215f7368927a0a9d6" },
			{ "294f22c5932794232f480d4b97d1291de8b3ab24", "e02aff1787731eb0d62d3feb2940a906c15312df" },
			{ "1d86a7f9aafa7b00ab187ace80f468664c66c924", "bf02d38ec7892c236ea4b293274ea6a3c334a25d" },
			{ "287a822c675abc6308357941862beda584b38a68", "bf02d38ec7892c236ea4b293274ea6a3c334a25d" },
			{ "3e4a31ba50c4f768e53207843155793ca70b3ba1", "498d7dc446852be2217fba65d8c41a0242e7402a" },
			{ "0202f2d889312a2f8a1479686ebbafc67572bb36", "6e6ca6c3d8f9eaf6e2364f1fef07b3f472f0a846" },
			{ "eb7e197b44fe0299233d423ce8184ba0cffc3883", "636cdfa52826c133f89802fd4f8d0dabbec733b9" },
			{ "020b9078de2cdc245cc99df621722187e7680145", "9217ff3fe3ec1aad7b12d79198ffb9252d6908de" },
			{ "dd27d36fd41b06f247074c949c2478cdbba72468", "0678cb22c235366e6443f8ba36afc299093d457c" },
			{ "67859229b2af3ba90e55d42060c363e9c83849bf", "f2b1176c042dd0d792a0f916f759bd848210d4d8" },
			{ "6b42d12b8135b62734b087ee4b02cf28d259a483", "9837d3731bea1e0d3aaed58a46127574f76ffe53" },
			{ "d7aa4417de5aebb1d2374018c62148692ef32d8b", "7777929a1344ae3adddfcf7a297005c278038c36" },
			{ "210e727c30fedfe36428f457a5e2f9e6176680c4", "0107644769df870b4b9b5d39c23e022645709de5" },
			{ "23f519f064273e3cc195f02d4e0e49cf7c9329a8", "a6dd04483cb30505dd31342b8e0d790b3545179d" },
			{ "9cb315afb580ee4bef44ec55c62fba291b50e04b", "48dc1ee65779dbdc177bc16530afd4f43c1c0ecf" },
			{ "9d1c1517f49452038dfeb57497c491f93b1616c0", "1efaba7f56949957e2b13a8f2a59502184204c6d" },
			{ "7d8e7db38dc74a9a7ddcc48566f03f2b6af6f737", "9d40e17d44e193330835b996e62e268e9aa05d1b" },
			{ "7d8e7db38dc74a9a7ddcc48566f03f2b6af6f737", "23340c0f002bdb71a989789b894e159f81453bff" },
			{ "4adc95e616bac7eea015e9e47e439b063c1132d5", "2bbc8d558d247c6ef91207a12a4650c0bc292dd6" },
			{ "b16c2e4a254d31480561f2bca5aeaeb75328de9c", "4e32d3ada00dc86f30145cd90e757baad18afd5f" },
			{ "37930f0f2f3c60737f8ce994bd695a224792b7e2", "a2f19a19a2a93bd06c4d12944dde16be09025088" },
			{ "c24e7b67b597eaa15fbaca34ebb57a7cc242d937", "3dac15a5e55ca29c7a6db0641c9887b1384cb5fd" },
			{ "c8269701e1f9e7b1dcbe771b4e7a802f050436d4", "036d8bd42e6e430b0e760ef10edcff408b0b2527" },
			{ "0e985b6b1215714dc84881a833eeadc5ef85c455", "4411113f31b3f00171bb335092b02104d29d7cd7" },
			{ "289d21b27f6ed6d510e19182f97e90e2203be76c", "f7bbabd3deb33ca111eb6b17f0252ad07f079f16" },
			{ "9091a618773bfbb9dc94067f69bea900a2c09c1d", "4f187fc91707286d3f695e2ea5e34351c2516fc6" },
			{ "933981904d595f4bab92e904b4d426ca07c8ccfc", "5a33cafb3e9fc5707648ad1389fba19eefaf2247" },
			{ "54f7170d626da3cbbd288fe2b7c7dbb547cb80e8", "9a8a8154e760831a634ef0df130347674ea26459" },
			{ "b73790d3c9fb2026c569344bfe353223af9bdbea", "fa13de9435b2f7be1099b9202ae6e26021b00ba8" },
			{ "0e945d0082cb70df3b6da49ba9241db5ee15f208", "5bc8648cf6ac025ca43fa76c724554b043d49ab6" },
			{ "9a7d94ef19fc170c3cac472a31f467828efc1cd4", "532217f11892796c16c46ab116e3d28b314a2e17" },
			{ "ff2f7ef032add258929ac5ef7e2cb3680b318700", "2b4f847434de297901e2c76921bae9eecb9a8929" },
			{ "f1ce3bf1be19f4a550c630992f15e96258801164", "2b4f847434de297901e2c76921bae9eecb9a8929" },
			{ "423174046424f87a68e7227cd8015f1926fede2d", "9c955c4d43e6b85fa829444681b37167f0dea301" },
			{ "b953482e2d970eeb88de96a38c087d03db83a5cd", "eb78bf8f0da016f321bb20da1ca06461c3356b39" },
			{ "503b513ef40d22a4c2b669896bfe16079d756b69", "124653e52746add2710fcb1ef598fd0ee283007b" },
			{ "20ed56d5abd6d5f12a39534299b7fbba1916769c", "0aeed6507963171d23ad85fa5175ab17feb9a951" },
			{ "07962d9a9be57321e306c86241903a78862dad8e", "38ec24b4509c45dcd56747fd58a17bbb8960a9a9" },
			{ "e92f586bd3fd45336a351e251027401d8d4a7135", "8e3247042c02fd16ad4031fefd53fa013b28bc29" },
			{ "9e8c01f558a03902ff2f54935fd7e6dcc7ec656c", "2e0eeeaafce11cb0128a6d1e245f1a5b806e3a87" },
			{ "5ce5779215ef1e2ae554b5ea5a8c90b62c0729aa", "51393fc07355ffd0a4b6b212fd676ee37de23e09" },
			{ "23f0c133b39568de35784182a730cb2929c27e34", "a9a5a0dc04939697af9865334c538bca556f5472" },
			{ "5b3114678323b284f88ba8d2da3cad315a53ed6e", "19185491d7b98ad254c08bed49440bbf4e185376" },
			{ "4656de94bd0fe9964273dadbb6ad728a076eb4ee", "b9627ca0e5018428a0c9716cd6161b63cbc42db8" },
			{ "51f494ccbae191b4b6fd4232895ccf47ceb713e5", "89dbd0d1c0057e09d404ffc2731fbe3d87feea11" } };

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		long execStart = System.currentTimeMillis();

		File dir = new File("../../data/repositories/");
		File[] fileList = dir.listFiles();

		for (File file : fileList) {
			if (file.getName().endsWith(".xml") && ((file.getName().indexOf("perl") > -1)) || ((file.getName().indexOf("git") > -1))) {

				String[][] conflicts;
				if (file.getName().indexOf("perl") > -1)
					conflicts = perlConflicts;
				else if (file.getName().indexOf("git") > -1)
					conflicts = gitConflicts;
				else
					continue;

				VCSGraph graph = VCSGraph.readXML(file.getAbsolutePath());

				Collection<VCSNode> nodes = graph.getVertices();

				Vector<VCSNode> nodeList = new Vector<VCSNode>(nodes);

				Collections.sort(nodeList, new Comparator<VCSNode>() {

					@Override
					public int compare(VCSNode arg0, VCSNode arg1) {
						return arg0.getTime().compareTo(arg1.getTime());
					}
				});

				Date start = nodeList.firstElement().getTime();
				Date end = nodeList.lastElement().getTime();

				// System.out.println("File: "+file.getName());
				// System.out.println("\tstart: " + start + " hex: " + nodeList.firstElement().getHex());
				// System.out.println("\tend: " + end + " hex: " + nodeList.lastElement().getHex());

				int yellow = 0;
				int red = 0;
				int yellowtored = 0;
				int greentored = 0;
				int errors = 0;

				Set<VCSNodePair> specMerges = graph.getSpeculativeMerges();

				for (VCSNodePair merge : graph.getKnownMerges().toArray(new VCSNodePair[0])) {

					if (merge.hasConflict() || inConflictList(merge.first().getHex(), merge.second().getHex(), conflicts)) {
						try {
							red++;
							VCSNode onesOldestP = getOldestNonCommonParent(merge.first(), merge.second());
							VCSNode twosOldestP = getOldestNonCommonParent(merge.second(), merge.first());
							boolean everyellow = false;
							for (VCSNodePair specMerge : specMerges) {

								if (specMerge.first().equals(onesOldestP))
									if (!specMerge.hasConflict())
										everyellow = true;
								if (specMerge.first().equals(twosOldestP))
									if (!specMerge.hasConflict())
										everyellow = true;
								if (specMerge.second().equals(onesOldestP))
									if (!specMerge.hasConflict())
										everyellow = true;
								if (specMerge.second().equals(twosOldestP))
									if (!specMerge.hasConflict())
										everyellow = true;
							}
							if (everyellow)
								yellowtored++;
							else
								greentored++;

							// if (((specMerge.first().equals(onesOldestP)) && (specMerge.second().equals(twosOldestP))) ||
							// ((specMerge.second().equals(onesOldestP)) && (specMerge.first().equals(twosOldestP)))) {
							// if (specMerge.hasConflict())
							// greentored++;
							// else
							// yellowtored++;
							// } else {
							// errors++;
							// //throw new RuntimeException("I found a merge whose parents were not speculated");
							// }
						} catch (RuntimeException e) {
							errors++;
						}
					} else {
						yellow++;
					}
				}

				// VCSNodePair first = graph.getKnownMerges().toArray(new VCSNodePair[0])[0];
				// first.conflictSet();
				// first.first().getParents();

				long difference = (end.getTime() - start.getTime());
				long years = difference / 1000 / 3600 / 24;
				System.out.println("\tdiff (days): " + years);
				System.out.println("\tstart:" + start);
				System.out.println("\tend:" + end);
				System.out.println("yellow\t\t" + yellow + "\nred\t\t" + red + "\nyellowtored\t" + yellowtored + "\ngreentored\t" + greentored + "\nerrors\t\t" + errors+"\n");
			}
		}

		System.out.println("Done. Took: " + ((System.currentTimeMillis() - execStart) / 1000) + " seconds.");
	}

	private static boolean inConflictList(String hex1, String hex2, String[][] list) {
		for (String[] pair : list) {
			if (pair[0].equals(hex1) && pair[1].equals(hex2))
				return true;
			if (pair[1].equals(hex1) && pair[0].equals(hex2))
				return true;
		}
		return false;
	}

	private static VCSNode getOldestNonCommonParent(VCSNode one, VCSNode two) {
		Set<VCSNode> onesParentSet = getAllParents(one);
		Set<VCSNode> twosParentSet = getAllParents(two);

		onesParentSet.removeAll(twosParentSet);

		List<VCSNode> onesParentList = new ArrayList<VCSNode>(onesParentSet);
		Collections.sort(onesParentList, new Comparator<VCSNode>() {

			@Override
			public int compare(VCSNode arg0, VCSNode arg1) {
				return arg0.getTime().compareTo(arg1.getTime());
			}
		});
		return onesParentList.get(0);
	}

	// private static Set<VCSNode> getNonCommonParents(VCSNodePair pair) {
	// Set<VCSNode> onesParentSet= getAllParents(pair.first());
	// Set<VCSNode> twosParentSet= getAllParents(pair.second());
	//
	// return onesParentSet.removeAll(twosParentSet)
	// }

	private static Set<VCSNode> getAllParents(VCSNode node) {
		Set<VCSNode> parents = new HashSet<VCSNode>();
		parents.add(node);
		if ((node.getParents() != null) && (!node.getParents().isEmpty())) {
			parents.addAll(node.getParents());
			for (VCSNode parent : parents) {
				if (!parents.contains(parent))
					parents.addAll(getAllParents(parent));
			}
		}
		return parents;
	}

}
