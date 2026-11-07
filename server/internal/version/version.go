package version

import "fmt"

const (
	emptyVersion = "v0.0.0"
)

var Version, Revision string

func GetVersion() string {
	if Version != "" {
		return Version
	}

	if Revision != "" {
		return fmt.Sprintf("%v-%v", emptyVersion, Revision)
	}

	return emptyVersion
}
