Version = 0.03

Sources = onzen.tcl \
          onzen_conflict.tcl \
          onzen_repository.tcl \
          onzen_view.tcl \
          onzen_busyindicator.tcl \
          onzen_file.tcl \
          onzen_tag.tcl \
          onzen_config.tcl \
          onzen_preferences.tcl \
          onzen_update.tcl \
          \
          pkgIndex.tcl \
          tools.tcl \
          dialogs.tcl \
          localize.tcl \
          mclistbox.tcl \

onzen: $(Sources)
	freewrapBLT onzen.tcl

source-dist:
	tar czf onzen-$(Version).tgz $(Sources)

