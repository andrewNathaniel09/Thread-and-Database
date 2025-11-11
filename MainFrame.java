import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

// Kelas utama GUI yang menampilkan sistem antrian rumah sakit
public class MainFrame extends JFrame {
    private JTextArea logArea; // Area teks untuk menampilkan aktivitas petugas
    private JTable table;      // Tabel untuk menampilkan data antrian pasien dari database

    // Konstruktor utama GUI
    public MainFrame() {
        setTitle("🏥 Sistem Antrian Rumah Sakit Nusantara"); // Judul Window
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout()); // Layout utama GUI

        // Membuat tabel antrian berdasarkan model data TableModelAntrian
        table = new JTable(new TableModelAntrian());

        // Membuat area log untuk menampilkan proses simulasi
        logArea = new JTextArea(10, 30);
        logArea.setEditable(false); 
        JScrollPane scrollTable = new JScrollPane(table); 
        JScrollPane scrollLog = new JScrollPane(logArea);
        // Panel tombol di bagian atas GUI
        JPanel btnPanel = new JPanel();
        JButton startBtn = new JButton("Mulai Simulasi"); // Tombol untuk memulai simulasi

        // Event listener untuk tombol simulasi
        startBtn.addActionListener(e -> mulaiSimulasi());

        // Menambahkan tombol ke panel
        btnPanel.add(startBtn);

        // Menambahkan semua komponen ke dalam frame utama
        add(scrollTable, BorderLayout.CENTER); // Tabel di tengah
        add(scrollLog, BorderLayout.SOUTH);    // Log di bawah
        add(btnPanel, BorderLayout.NORTH);     // Tombol di atas

        //Setiap kali program dijalankan, meng set status pasien ke "Menunggu"
        resetStatusSaatMulai();

        ((TableModelAntrian) table.getModel()).refreshData();
    }

    private void resetStatusSaatMulai() {
        try (Connection conn = dbConn.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = "UPDATE antrian SET status = 'Menunggu' WHERE status IN ('Selesai', 'Dilayani')";
            stmt.executeUpdate(sql);

            System.out.println("✅ Semua status pasien telah direset menjadi 'Menunggu'.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Fungsi untuk memulai simulasi petugas (dijalankan ketika tombol ditekan)
    private void mulaiSimulasi() {
        logArea.setText("");

        // Menjalankan tiga thread petugas sekaligus (simulasi paralel)
        new Petugas("Petugas-1", logArea, table).start();
        new Petugas("Petugas-2", logArea, table).start();
        new Petugas("Petugas-3", logArea, table).start();
    }

    //Kelas internal untuk mengatur data tabel pasien yang ditampilkan di GUI
    public static class TableModelAntrian extends AbstractTableModel {
        private Vector<Vector<Object>> data = new Vector<>(); //
        private String[] columns = {"ID Antrian", "Nama Pasien", "Keluhan", "Status"};

        public TableModelAntrian() {
            refreshData();
        }

        //Method untuk memuat ulang data dari database ke tabel GUI
        public void refreshData() {
            data.clear();
            try (Connection conn = dbConn.getConnection()) {
                String query = "SELECT a.id_antrian, p.nama_pasien, p.keluhan, a.status " +
                               "FROM antrian a JOIN pasien p ON a.id_pasien = p.id_pasien";
                ResultSet rs = conn.createStatement().executeQuery(query);

                // Loop setiap baris hasil query dan tambahkan ke tabel
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("id_antrian"));
                    row.add(rs.getString("nama_pasien"));
                    row.add(rs.getString("keluhan"));
                    row.add(rs.getString("status"));
                    data.add(row);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return data.size(); // Jumlah baris
        }

        @Override
        public int getColumnCount() {
            return columns.length; // Jumlah kolom
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return data.get(rowIndex).get(columnIndex); // Ambil nilai sel
        }

        @Override
        public String getColumnName(int col) {
            return columns[col]; // Nama kolom
        }
    }
}
