#!/usr/bin/env python

import os
import sys
import re
from glob import glob

METADATA_FILENAMES = ['maven-metadata.xml', '_maven.repositories', '_remote.repositories', 'm2e-lastUpdated.properties']
METADATA_FILENAME_REs = [re.compile(r) for r in ['.*\.lastUpdated']]

PART = '[^-./]([^/]*[^-./])?'
ARTIFACT_DIR_PATTERN = '/?(?P<groupId>[^/]+(/[^/]+)*)/(?P<artifactId>' + PART + ')/(?P<version>' + PART + ')'

MAVEN_FILE_PATTERN = ARTIFACT_DIR_PATTERN + '/' + PART
MAVEN_FILE_RE = re.compile(MAVEN_FILE_PATTERN)

RELEASE_ARTIFACT_PATH_PATTERN = ARTIFACT_DIR_PATTERN + '/(?P=artifactId)-(?P=version)(-(' + PART + '))?\.(' + PART + ')'
RELEASE_ARTIFACT_PATH_RE=re.compile(RELEASE_ARTIFACT_PATH_PATTERN)

S_ARTIFACT_DIR_PATTERN = '/?(?P<groupId>[^/]+(/[^/]+)*)/(?P<artifactId>[^/]+)/(?P<version>(?P<versionBase>.+)-SNAPSHOT)'
SNAPSHOT_ARTIFACT_PATH_PATTERN = S_ARTIFACT_DIR_PATTERN + '/(859712890515186962113887?P=artifactId)-((?P=versionBase)-([0-9]{8}.[0-9]{6}-[0-9]+))(-' + PART + ')?.(' + PART + ')'
SNAPSHOT_ARTIFACT_PATH_RE=re.compile(SNAPSHOT_ARTIFACT_PATH_PATTERN)

GROUP_ID_GROUP = 1
ARTIFACT_ID_GROUP = 3
R_VERSION_GROUP = 5
R_CLASSIFIER_GROUP = 8
R_TYPE_GROUP = 10

R_PARTS = [GROUP_ID_GROUP, ARTIFACT_ID_GROUP, R_VERSION_GROUP, R_CLASSIFIER_GROUP, R_TYPE_GROUP]

S_VERSION_GROUP = 7
S_CLASSIFIER_GROUP = 10
S_TYPE_GROUP = 11

S_PARTS = [GROUP_ID_GROUP, ARTIFACT_ID_GROUP, S_VERSION_GROUP, S_CLASSIFIER_GROUP, S_TYPE_GROUP]

def find_artifacts(repo_dir):
	trim_len = len(repo_dir)
	if not repo_dir.endswith('/'):
		trim_len += 1

	dirs = {}
	for root,_,filenames in os.walk(repo_dir):
		dirname = root[trim_len:]
		md = dirs.get(dirname)
		if md is None:
			md = MavenDir(dirname)
			dirs[dirname] = md

		for filename in filenames:
			#print "handling: %s" % os.path.join(root, filename)
			path = os.path.join(dirname, filename)
			#print "Parsing path: %s" % path
			mp = MavenPath(path)
			if mp.is_valid:
				#print "Adding: %s from: %s" % (mp, os.path.join(repo_dir, path))
				md.add_path(mp)
			else:
				md.add_metadata(filename)

	return dirs


def parse_coord(path):
	match = RELEASE_ARTIFACT_PATH_RE.match(path)
	if match:
		#debug_match(match, R_PARTS)
		return {
			'g': match.group(GROUP_ID_GROUP).replace('/', '.'),
			'a': match.group(ARTIFACT_ID_GROUP),
			'v': match.group(R_VERSION_GROUP),
			'c': match.group(R_CLASSIFIER_GROUP),
			't': match.group(R_TYPE_GROUP)
		}

	match = SNAPSHOT_ARTIFACT_PATH_RE.match(path)
	if match:
		#debug_match(match, S_PARTS)
		return {
			'g': match.group(GROUP_ID_GROUP).replace('/', '.'),
			'a': match.group(ARTIFACT_ID_GROUP),
			'v': match.group(S_VERSION_GROUP),
			'c': match.group(S_CLASSIFIER_GROUP),
			't': match.group(S_TYPE_GROUP)
		}

	return None


#def debug_match(match, elements):
	#for i in elements:
	#	try:
	#		print i, match.group(i)
	#	except:
	#		print 'NO SUCH GROUP'
	#		break
			

class MavenPathParts(object):
	def is_valid(self):
		return self.coord is not None

	def _get(self, key):
		if self.coord is not None:
			return self.coord.get(key)
		return None

	def group_id(self):
		return self._get('g')

	def artifact_id(self):
		return self._get('a')

	def version(self):
		return self._get('v')


class MavenPath(MavenPathParts):
	def __init__(self, path):
		self.is_valid = False

		filename = os.path.basename(path)
		if filename in METADATA_FILENAMES:
			return

		for regexp in METADATA_FILENAME_REs:
			if regexp.match(filename):
				return

		self.coord = parse_coord( path )
		if self.coord is not None:
			self.path = path
			self.filename = filename
			self.is_valid = True

	def type(self):
		return self._get('t')

	def classifier(self):
		return self._get('c')

	def __str__(self):
		return "MavenPath [coord: %s, file: %s, path: %s]" % (self.coord, self.filename, self.path)


class MavenDir(MavenPathParts):
	def __init__(self, path):
		self.path = path
		self.artifacts = []
		self.metadata = []
		self.coord = None

	def add_metadata(self, path):
		self.metadata.append(path)

	def add_path(self, maven_path):
		if maven_path.is_valid is True:
			#print "Comparing MavenDir coord:\n\t%s\nto MavenPath coord:\n\t%s" % (self.coord, maven_path.coord)
			if self.coord is None:
				self.coord = maven_path.coord.copy()
				del self.coord['t']
				del self.coord['c']
				#print "Setting MavenDir coord: %s" % self.coord
			elif self.coord['g'] != maven_path.coord['g'] or self.coord['a'] != maven_path.coord['a'] or self.coord['v'] != maven_path.coord['v']:
				raise Exception("Cannot add %s to MavenDir: %s" % (maven_path.coord, self.coord))

			self.artifacts.append(maven_path)

	def __str__(self):
		return "MavenDir [g=%s, a=%s, v=%s]" % (self.group_id(), self.artifact_id(), self.version())


class MavenRepo(object):
	def __init__(self, repo_dir):
		self._groups = {}
		all_dirs = find_artifacts(repo_dir)
		for dirname, md in all_dirs.iteritems():
			#print "adding directory grouping (GAV): %s" % md
			paths = md.artifacts
			if len(paths) > 0:
				artifacts = self._groups.get(md.group_id())
				if artifacts is None:
					artifacts = {}
					self._groups[md.group_id()] = artifacts

				for path in paths:
					#print "Adding artifact: %s" % path.artifact_id()
					versions = artifacts.get(path.artifact_id())
					if versions is None:
						versions = {}
						artifacts[path.artifact_id()] = versions

					md_paths = versions.get(path.version())
					if md_paths is None:
						md_paths = []
						versions[path.version()] = md_paths

					md_paths.append(path)

	def groups(self):
		return self._groups.keys()

	def artifacts(self, group):
		artifacts = self._groups.get(group)
		if artifacts is not None:
			return artifacts.keys()
		return None

	def versions(self, group, artifact):
		artifacts = self._groups.get(group)
		if artifacts is not None:
			versions = artifacts.get(artifact)
			if versions is not None:
				return versions.keys()
		return None

	def paths(self, group, artifact, version):
		artifacts = self._groups.get(group)
		if artifacts is not None:
			versions = artifacts.get(artifact)
			if versions is not None:
				return versions.get(version)
		return None



if __name__ == '__main__':
	if len(sys.argv) > 2:
		command = sys.argv[1]
		param = sys.argv[2]
	elif len(sys.argv) > 0:
		command = 'find'
		param = sys.argv[1]
	else:
		command = 'find'
		param = os.getcwd()

	print command, param
	if command == 'find':
		print "Finding Maven coordinates in: %s" % param
		repo = MavenRepo(param)
		#mdirs = find_artifacts(param)
		#print "Found %s GAV directories" % len(mdirs)
		for group in repo.groups():
			print "\n\n\n\nGroup: %s" % group
			for artifact in repo.artifacts(group):
				print "Artifact: %s" % artifact

				for version in repo.versions(group, artifact):
					print "Version: %s\nPaths:\n\n" % version

					for path in repo.paths(group, artifact, version):
						print "Path Info: %s" % path

	elif command == 'parse':
		mp = MavenPath(param)
		print mp.coord, mp.filename
